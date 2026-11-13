package com.kodegravity.exr.catalog;

import com.kodegravity.exr.catalog.domain.Event;
import com.kodegravity.exr.catalog.domain.Source;
import com.kodegravity.exr.catalog.infra.EventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The N+1 problem, counted rather than described.
 *
 * <p>Each test runs the same query twice over the same data and reads Hibernate's
 * statement counter, so "one query instead of four" is a number on screen rather
 * than a claim.
 */
@SpringBootTest(properties = {
        "spring.jpa.show-sql=true",
        // the counters read below are only collected when this is switched on
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@Transactional
class EventFetchingTest extends AbstractIntegrationTest {

    @Autowired
    private EventRepository events;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /**
     * Three events, deliberately from three DIFFERENT sources (ids 1, 2 and 3,
     * seeded by V1). That detail matters: the persistence context caches a source
     * by id, so three events sharing one supplier would cost a single extra query
     * and hide the problem being demonstrated.
     */
    @BeforeEach
    void seed_three_events_from_three_sources() {
        persist("ext-tmdb", (short) 1);
        persist("ext-igdb", (short) 2);
        persist("ext-seed", (short) 3);
        entityManager.flush();
        entityManager.clear();   // nothing cached — the queries below must reach Postgres
    }

    private void persist(String externalId, short sourceId) {
        Source source = entityManager.find(Source.class, sourceId);
        Event event = new Event(UUID.randomUUID(), source, externalId, "GAME",
                "Test " + externalId, LocalDate.of(2026, 1, 1), "DAY");
        entityManager.persist(event);
    }

    /** Zeroed at the start of each test, so only that test's SQL is counted. */
    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    void without_a_fetch_plan_the_source_costs_one_query_per_event() {
        statistics().clear();

        List<Event> found = events.findByCategory("GAME");
        found.forEach(event -> event.getSource().getCode());   // touch the LAZY field

        long statements = statistics().getPrepareStatementCount();
        System.out.println("=== STATEMENTS without @EntityGraph = " + statements
                + "  (1 for the events + " + found.size() + " for their sources)");

        assertThat(statements).isEqualTo(1 + found.size());
    }

    @Test
    void an_entity_graph_fetches_the_source_in_the_same_statement() {
        statistics().clear();

        List<Event> found = events.findWithSourceByCategory("GAME");
        found.forEach(event -> event.getSource().getCode());

        long statements = statistics().getPrepareStatementCount();
        System.out.println("=== STATEMENTS with @EntityGraph ==== " + statements);

        assertThat(found).hasSize(3);
        assertThat(statements).isEqualTo(1);
    }
}
