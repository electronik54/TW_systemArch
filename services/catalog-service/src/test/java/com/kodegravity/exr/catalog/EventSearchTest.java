package com.kodegravity.exr.catalog;

import com.kodegravity.exr.catalog.domain.Event;
import com.kodegravity.exr.catalog.domain.Source;
import com.kodegravity.exr.catalog.infra.EventSearchRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-text search, proven three ways instead of described.
 *
 * <p>Each test below answers a question the migration's comments CLAIM:
 * <ol>
 *   <li>does a title match really beat a synopsis match?</li>
 *   <li>does a one-word title like "It" survive the indexing at all?</li>
 *   <li>is the GIN index actually used, or is this a full scan wearing a
 *       search feature's clothes?</li>
 * </ol>
 *
 * <p>Everything runs inside one transaction, so the rows written by
 * {@code @BeforeEach} are on the same connection the search query uses — the
 * query sees them without needing a commit.
 */
@Transactional
class EventSearchTest extends AbstractIntegrationTest {

    @Autowired
    private EventSearchRepository search;

    @Autowired
    private EntityManager entityManager;

    /** Used for the EXPLAIN only — the test plays the role of the query planner's audience. */
    @Autowired
    private JdbcTemplate jdbc;

    private UUID duneAsTitle;
    private UUID duneInSynopsis;
    private UUID it;

    @BeforeEach
    void seed() {
        duneAsTitle = persist("ext-dune", "Dune",
                "A noble family is destroyed on a desert planet.");
        duneInSynopsis = persist("ext-arrival", "Arrival",
                "A linguist learns to read the dunes of time.");
        it = persist("ext-it", "It",
                "A clown waits in the sewers of Derry."); // adds to memory | No immediate SQL execution | IF your entity uses @GeneratedValue(strategy = GenerationType.IDENTITY), INSERT is immediate
        entityManager.flush(); // adds to DB | Executes SQL
        entityManager.clear(); // clear in-memory data | No database updates
    }

    private UUID persist(String externalId, String title, String synopsis) {
        Source source = entityManager.find(Source.class, (short) 1);
        Event event = new Event(UUID.randomUUID(), source, externalId, "MOVIE",
                title, LocalDate.of(2026, 1, 1), "DAY");
        event.setSynopsis(synopsis);
        entityManager.persist(event);
        return event.getId();
    }

    @Test
    @DisplayName("a match in the title outranks a match in the synopsis")
    void titleOutranksSynopsis() {
        List<EventSearchRepository.Hit> hits = search.search("dune", 10);

        hits.forEach(hit -> System.out.println("=== HIT " + hit.title() + "  score=" + hit.score()));

        // "Dune" (title, weight A, simple) vs "dunes" (synopsis, weight B, english)
        assertThat(hits).extracting(EventSearchRepository.Hit::id)
                .containsExactly(duneAsTitle, duneInSynopsis);
        assertThat(hits.get(0).score()).isGreaterThan(hits.get(1).score());
    }

    @Test
    @DisplayName("\"It\" is still findable, because the title is indexed with 'simple'")
    void shortTitleSurvives() {
        // 'english' alone would answer "it" with an EMPTY query - it is a stopword -
        // and the film would be invisible under its own name. The 'simple' pass is
        // what keeps it here.
        assertThat(search.search("It", 10))
                .extracting(EventSearchRepository.Hit::id)
                .contains(it);
    }

    @Test
    @DisplayName("a blank term costs nothing and matches nothing")
    void blankTermReturnsEmpty() {
        assertThat(search.search("   ", 10)).isEmpty();
        assertThat(search.search(null, 10)).isEmpty();
    }

    @Test
    @DisplayName("the GIN index answers the filter, not a full scan")
    void ginIndexIsUsed() {
        // Three rows is far too few for the planner to prefer an index on merit:
        // reading three rows is cheaper than opening an index at all. Forcing the
        // sequential scan's cost out of the picture makes the question askable -
        // "COULD the index answer this?" - which is the part that breaks the day
        // somebody drops it.
        jdbc.execute("set local enable_seqscan = off");

        String plan = String.join("\n", jdbc.queryForList(
                "explain select id from events "
                        + "where search_vector @@ websearch_to_tsquery('simple', 'dune')",
                String.class));

        System.out.println("=== EXPLAIN ===\n" + plan);

        assertThat(plan).contains("events_search_gin");
    }
}
