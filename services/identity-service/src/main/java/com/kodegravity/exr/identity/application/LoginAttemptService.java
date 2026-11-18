package com.kodegravity.exr.identity.application;

import com.kodegravity.exr.identity.domain.User;
import com.kodegravity.exr.identity.infra.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Where a login attempt is counted, and the only place that decides what "too many
 * failures" means.
 *
 * <p><b>Why this is a service and not two lines in an event listener:</b> the
 * numbers are a product decision that an operator may need to change without
 * touching security wiring, and the write must happen in its own transaction (see
 * below). A listener is plumbing; the rule lives here.
 *
 * <p><b>Why {@code @Transactional} is essential here, not decorative.</b> These
 * methods are called from Spring Security's event bus, at a moment when
 * <em>no</em> transaction is open - unlike a controller call. Without the
 * annotation, the entity would be loaded, changed, and never written: Hibernate
 * would have nobody to flush to. The counter would silently never increment, and
 * lockout would never fire - a security control that looks present and does
 * nothing.
 *
 * <p><b>Why not {@code REQUIRES_NEW}.</b> Day 2.7 taught it as the tool for
 * "commit even though the caller rolls back". Here the caller has no transaction
 * at all, so a plain {@code REQUIRED} starts one and there is nothing to survive.
 * Reach for {@code REQUIRES_NEW} only when an outer transaction really exists.
 *
 * <p><b>What this deliberately does not do:</b> tell the caller whether the
 * address exists. A failure for an unknown email is a no-op here; the client gets
 * the same 401 either way.
 */
@Service
public class LoginAttemptService {

    /** Five wrong guesses is the usual compromise: generous for a typo, useless for a script. */
    public static final int MAX_ATTEMPTS = 5;

    /**
     * Fifteen minutes is long enough that a 5-guess loop makes no progress, short
     * enough that a locked-out user can wait rather than call support. A permanent
     * lock would be a denial-of-service lever.
     */
    public static final Duration LOCK_FOR = Duration.ofMinutes(15);

    private final UserRepository users;

    public LoginAttemptService(UserRepository users) {
        this.users = users;
    }

    /** Counts one failure for this email, if the account exists. */
    @Transactional
    public void recordFailure(String email) {
        users.findByEmail(email)
                .ifPresent(user -> user.recordFailedLogin(MAX_ATTEMPTS, LOCK_FOR));
    }

    /** Wipes the counter: the owner just proved they know the password. */
    @Transactional
    public void recordSuccess(String email) {
        users.findByEmail(email).ifPresent(User::recordSuccessfulLogin);
    }
}
