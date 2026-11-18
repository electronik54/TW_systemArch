package com.kodegravity.exr.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The lockout rule, tested without a database, a Spring context or a password.
 *
 * <p><b>What this class protects.</b> Three lines of {@link User} decide whether a
 * brute-force attack is slowed down: the counter rises, the fifth rise writes a
 * deadline, and an expired deadline resets the run. They are pure arithmetic on two
 * fields, so a test here is milliseconds long and needs neither Docker nor Testcontainers
 * - the right <em>kind</em> of test for a business rule, as opposed to the integration
 * tests that prove the column types match the migration.
 *
 * <p><b>Why a plain object and not the entity from a database.</b> The rule does not
 * read or write anything outside itself: given a counter and a clock, the answer is
 * fixed. Pulling a Postgres container in would test Hibernate, not the rule.
 *
 * <p><b>What it deliberately does not test.</b> That Spring refuses a locked account -
 * that happens in {@code DbUserDetailsService} and in Spring's own
 * {@code DaoAuthenticationProvider}, and it belongs to a slice test with a real request.
 * Here the question is only "does the row know it is locked?".
 */
class UserLockoutTest {

    /** The same two numbers the service passes in, so the test fails if someone changes one silently. */
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_FOR = Duration.ofMinutes(15);

    /** A user whose hash is never compared: no password is involved in these tests. */
    private static User newUser() {
        return new User(UUID.randomUUID(), "lockout-test@example.com",
                "hash-unused-in-this-test", UserRole.FREE_USER);
    }

    @Test
    @DisplayName("the fourth failure counts but does not lock; the fifth writes the deadline")
    void fifthFailureWritesTheDeadline() {
        User user = newUser();

        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            user.recordFailedLogin(MAX_ATTEMPTS, LOCK_FOR);
        }

        // Four failures: the counter moves, the door is still open.
        assertThat(user.getFailedAttempts()).isEqualTo(4);
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.isTemporarilyLocked(Instant.now())).isFalse();

        user.recordFailedLogin(MAX_ATTEMPTS, LOCK_FOR);

        // Five: the counter is at the limit and a deadline exists, about 15 minutes ahead.
        assertThat(user.getFailedAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.isTemporarilyLocked(Instant.now())).isTrue();
        assertThat(user.getLockedUntil())
                .isAfter(Instant.now().plus(Duration.ofMinutes(14)))
                .isBefore(Instant.now().plus(Duration.ofMinutes(16)));
    }

    @Test
    @DisplayName("the deadline expires on its own: same row, later clock, no write in between")
    void deadlineExpiresByItself() {
        User user = newUser();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            user.recordFailedLogin(MAX_ATTEMPTS, LOCK_FOR);
        }

        Instant now = Instant.now();

        // Nothing changes on the object between these two calls - only the argument.
        assertThat(user.isTemporarilyLocked(now)).isTrue();
        assertThat(user.isTemporarilyLocked(now.plus(Duration.ofMinutes(16)))).isFalse();
    }

    @Test
    @DisplayName("a success clears both the counter and the deadline")
    void successClearsEverything() {
        User user = newUser();
        user.recordFailedLogin(MAX_ATTEMPTS, LOCK_FOR);
        user.recordFailedLogin(MAX_ATTEMPTS, LOCK_FOR);

        user.recordSuccessfulLogin();

        assertThat(user.getFailedAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("a failure after an expired deadline is failure number one, not number six")
    void expiredDeadlineStartsAFreshRun() {
        User user = newUser();

        // One attempt allowed and a negative duration: the door locks immediately and the
        // deadline it writes is already in the past. That is how this test reaches the
        // "expired" branch without waiting fifteen minutes or faking the clock.
        user.recordFailedLogin(1, Duration.ofMinutes(-1));
        assertThat(user.isTemporarilyLocked(Instant.now())).isFalse();

        user.recordFailedLogin(MAX_ATTEMPTS, LOCK_FOR);

        // The previous run already finished its sentence, so this is a new run of one.
        assertThat(user.getFailedAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
    }
}
