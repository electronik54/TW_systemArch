package com.kodegravity.exr.catalog;

import com.kodegravity.exr.catalog.application.EventService;
import com.kodegravity.exr.catalog.domain.Event;
import com.kodegravity.exr.catalog.domain.Source;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Proves the transaction boundary in {@code EventService} is real, by watching
 * the table instead of trusting the annotation.
 *
 * <p><b>Why this class is deliberately NOT {@code @Transactional}</b> — the one
 * fact that makes this test meaningful. Every other test in this package carries
 * {@code @Transactional}, and Spring wraps the whole test method in a single
 * transaction that is rolled back at the end. Under that wrapper the service's
 * own commit is a no-op (its "transaction" joins the test's), so a test could
 * assert anything and still pass. Here the service must be allowed to commit for
 * real; the price is that this class cleans up after itself in {@code @AfterEach}.
 *
 * <p>Two claims are checked:
 * <ol>
 *   <li>a successful call is really committed — the rows are visible from a
 *       connection that never took part in the service's transaction;</li>
 *   <li>a clashing batch leaves <b>nothing</b> behind, including the row that was
 *       already accepted before the clash.</li>
 * </ol>
 */
class EventServiceTransactionTest extends AbstractIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private EntityManager entityManager;

    /** A separate connection to the same database — the outside observer. */
    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        // No rollback to lean on, so the test removes its own rows by name.
        // import_attempts is emptied too: the failure path writes there now, and a
        // leftover row would make the next run's count wrong.
        jdbc.update("delete from import_attempts");
        jdbc.update("delete from events where external_id like 'svc-test-%'");
    }

    @Test
    @DisplayName("a successful import is committed and visible from outside the service")
    void successfulImportCommits() {
        eventService.importAll((short) 1, List.of(
                draft("svc-test-a", "First"),
                draft("svc-test-b", "Second")));

        // Reached only if the proxy committed: jdbc is a different connection and
        // cannot see uncommitted writes from another transaction.
        assertThat(count("svc-test-a")).isEqualTo(1);
        assertThat(count("svc-test-b")).isEqualTo(1);
        assertThat(countAttempts()).isZero();
    }

    @Test
    @DisplayName("a clash rolls the whole batch back - the first event does not survive")
    void clashRollsBackEverything() {
        // 1. One event exists, committed on its own.
        eventService.importAll((short) 1, List.of(draft("svc-test-taken", "Already here")));
        assertThat(count("svc-test-taken")).isEqualTo(1);

        // 2. A batch whose FIRST event is fine and whose SECOND clashes on
        //    (source_id, external_id).
        Throwable failure = catchThrowable(() -> eventService.importAll((short) 1, List.of(
                draft("svc-test-fresh", "Should never land"),
                draft("svc-test-taken", "Duplicate"))));

        System.out.println("=== FAILURE " + failure.getClass().getName()
                + ": " + failure.getMessage().split("\\R")[0]);

        // The exact class depends on where Hibernate notices the clash - during the
        // flush at commit, not at persist() inside the loop. That is the point.
        assertThat(failure).isInstanceOf(RuntimeException.class);

        // 3. The promise: the fresh event was accepted by persist() and still is
        //    not in the table.
        assertThat(count("svc-test-fresh")).isZero();
        assertThat(count("svc-test-taken")).isEqualTo(1);

        // 4. THE POINT OF THIS STEP: one finished audit row survived, written in
        //    its own transaction (REQUIRES_NEW) while this one was failing. If the
        //    annotation were missing, this count would be 0 - the row would have
        //    rolled back with the events.
        assertThat(countFailedAttempts()).isEqualTo(1);
    }

    private int countFailedAttempts() {
        Integer rows = jdbc.queryForObject(
                "select count(*) from import_attempts where failed_at is not null", Integer.class);
        return rows == null ? 0 : rows;
    }

    private int countAttempts() {
        Integer rows = jdbc.queryForObject("select count(*) from import_attempts", Integer.class);
        return rows == null ? 0 : rows;
    }

    private int count(String externalId) {
        Integer rows = jdbc.queryForObject(
                "select count(*) from events where external_id = ?", Integer.class, externalId);
        return rows == null ? 0 : rows;
    }

    private Event draft(String externalId, String title) {
        Source source = entityManager.find(Source.class, (short) 1);
        return new Event(UUID.randomUUID(), source, externalId, "MOVIE",
                title, LocalDate.of(2026, 3, 1), "DAY");
    }
}
