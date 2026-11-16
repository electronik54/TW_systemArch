package com.kodegravity.exr.catalog.infra;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Free-text search over {@code events}, in plain SQL.
 *
 * <p>Why this is not a {@code JpaRepository}: Hibernate has no Java type for
 * <code>tsvector</code>, and <code>ts_rank()</code> is a Postgres function that
 * JPQL cannot call. Teaching Hibernate a custom {@code UserType} to read three
 * columns would add a mapping to maintain and would still need a native query —
 * so the query goes straight to JDBC and the result comes back as a small
 * record. That also keeps {@code ddl-auto: validate} honest: the entity maps
 * the 33 columns a human writes, and the 34th (the generated vector) stays the
 * database's business.
 *
 * <p>The SQL is the same shape as the sample at the bottom of
 * {@code V2__event_search.sql}:
 * <ul>
 *   <li><b>two questions, one filter</b> — the title was indexed under
 *       <code>simple</code> and the synopsis under <code>english</code>, so the
 *       term is turned into two queries and either may match;</li>
 *   <li><b>the better score wins</b> — {@code greatest(...)} of the two
 *       <code>ts_rank</code> values, so a title hit outranks a synopsis hit;</li>
 *   <li><b>rank is computed, not read</b> — the GIN index narrows the rows,
 *       then the survivors are scored and sorted. An index never returns rows in
 *       relevance order.</li>
 * </ul>
 */
@Repository
public class EventSearchRepository {

    private static final String SEARCH_SQL = """
            SELECT e.id,
                   coalesce(e.title_override, e.title) AS title,
                   greatest(
                       ts_rank(e.search_vector, websearch_to_tsquery('simple',  :term)),
                       ts_rank(e.search_vector, websearch_to_tsquery('english', :term))
                   ) AS score
            FROM events e
            WHERE e.search_vector @@ websearch_to_tsquery('simple',  :term)
               OR e.search_vector @@ websearch_to_tsquery('english', :term)
            ORDER BY score DESC, title ASC
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public EventSearchRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns the best {@code limit} matches for the user's words, best first.
     *
     * <p>{@code websearch_to_tsquery} is fed the raw text on purpose: it
     * understands what people type — <code>"exact phrase"</code>, and
     * <code>-word</code> to exclude — and it never throws on junk input, it just
     * produces an empty query. Which is why a blank term is short-circuited
     * here: an empty query matches nothing anyway, so there is no reason to
     * pay for the round trip.
     *
     * <p>The record is a projection, not an entity: a search result needs the id
     * (to link), the effective title (to show) and the score (to order). Loading
     * 33 columns plus a lazy supplier to display eight characters is the kind of
     * waste that only shows up under load.
     */
    public List<Hit> search(String term, int limit) {
        if (term == null || term.isBlank()) {
            return List.of();
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("term", term)
                .addValue("limit", limit);

        return jdbc.query(SEARCH_SQL, parameters, (rs, rowNumber) -> new Hit(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getDouble("score")));
    }

    /** One search result: what to link to, what to show, how good it was. */
    public record Hit(UUID id, String title, double score) {
    }
}
