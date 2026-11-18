package com.kodegravity.exr.catalog;

import com.kodegravity.exr.catalog.domain.Event;
import com.kodegravity.exr.catalog.domain.Source;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step 3 of Day 2.7: <b>which kind of exception undoes the work.</b>
 *
 * <p><b>The rule in one line:</b> Spring rolls back for a {@code RuntimeException} or
 * an {@code Error}, and <b>commits</b> for every other exception — which is to say,
 * for every checked one — unless the annotation names it:
 * {@code @Transactional(rollbackFor = Exception.class)}.
 *
 * <p><b>Why the default is the surprising way round.</b> The rule was set in the EJB
 * era, where a checked exception meant "the caller can do something about this", so
 * the work already done was assumed worth keeping. A runtime exception meant a bug,
 * and a bug's half-finished work is not worth keeping. The reasoning survived; the
 * consequence is a trap. A method that throws {@code IOException} after writing half
 * a batch has just <b>committed</b> half a batch, and the caller's {@code catch} sees
 * an error while the database holds the evidence.
 *
 * <p><b>What this class proves, two ways.</b> Same row, same exception, two
 * annotations:
 * <ol>
 *   <li>{@code plain(...)} — plain {@code @Transactional} — the row is <b>in the
 *       table</b> afterwards, counted from a second connection;</li>
 *   <li>{@code rollingBack(...)} — same code plus {@code rollbackFor} — the row is
 *       <b>gone</b>.</li>
 * </ol>
 *
 * <p><b>Why a little service of its own instead of {@code EventService}.</b> The
 * annotation works only through the proxy, and a proxy exists only for a Spring bean,
 * so the demonstration needs a bean. It is registered through {@link TestConfiguration}
 * and {@code @Import} on purpose: that keeps it out of the shared context, so no other
 * test in this module sees a class whose only job is to throw.
 *
 * <p><b>What our own code does about it.</b> Every business error in this project
 * extends {@code RuntimeException} ({@code WeakPasswordException},
 * {@code EmailAlreadyUsedException}) so that the safe default applies without anyone
 * remembering {@code rollbackFor}. A checked exception inside a transaction should be
 * a decision, not an accident.
 */
@Import(CheckedExceptionCommitTest.BoomBeans.class)
class CheckedExceptionCommitTest extends AbstractIntegrationTest {

    @Autowired
    private BoomService boomService;

    /** The outside observer: a second connection, so it cannot see uncommitted writes. */
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void cleanUp() {
        // Not @Transactional either: the bean really commits or really rolls back, so
        // there is no free cleanup to lean on.
        jdbc.update("delete from events where external_id like 'chk-test-%'");
    }

    @Test
    @DisplayName("a checked exception COMMITS the work done before it")
    void checkedExceptionCommitsByDefault() {
        Throwable thrown = catchThrowable(() -> boomService.plain(draft("chk-test-plain")));

        assertThat(thrown).isInstanceOf(IOException.class);

        // The trap, visible: the caller was told it failed, and the row is there.
        assertThat(count("chk-test-plain")).isEqualTo(1);
    }

    @Test
    @DisplayName("rollbackFor turns the same checked exception into a rollback")
    void rollbackForMakesItRollBack() {
        Throwable thrown = catchThrowable(() -> boomService.rollingBack(draft("chk-test-rollback")));

        assertThat(thrown).isInstanceOf(IOException.class);

        assertThat(count("chk-test-rollback")).isZero();
    }

    private int count(String externalId) {
        Integer rows = jdbc.queryForObject(
                "select count(*) from events where external_id = ?", Integer.class, externalId);
        return rows == null ? 0 : rows;
    }

    private Event draft(String externalId) {
        Source source = entityManager.find(Source.class, (short) 1);
        return new Event(UUID.randomUUID(), source, externalId, "MOVIE",
                "Checked-exception probe", LocalDate.of(2026, 4, 1), "DAY");
    }

    /** Only this class's context receives the bean - see the class javadoc. */
    @TestConfiguration
    static class BoomBeans {

        @Bean
        BoomService boomService(EntityManager entityManager) {
            return new BoomService(entityManager);
        }
    }

    /**
     * Writes one row, then throws a <b>checked</b> exception. The only difference
     * between the two public methods is the annotation on the second.
     */
    static class BoomService {

        private final EntityManager entityManager;

        BoomService(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @Transactional
        public void plain(Event event) throws IOException {
            persistThenFail(event);
        }

        @Transactional(rollbackFor = Exception.class)
        public void rollingBack(Event event) throws IOException {
            persistThenFail(event);
        }

        private void persistThenFail(Event event) throws IOException {
            entityManager.persist(event);
            // Same reason as in EventService: without the flush the INSERT would travel
            // at commit, after the throw, and this test would prove nothing.
            entityManager.flush();
            throw new IOException("the supplier answered 200 with a corrupt page");
        }
    }
}
