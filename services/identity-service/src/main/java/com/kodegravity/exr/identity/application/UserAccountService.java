package com.kodegravity.exr.identity.application;

import com.kodegravity.exr.identity.domain.EmailAlreadyUsedException;
import com.kodegravity.exr.identity.domain.PasswordPolicy;
import com.kodegravity.exr.identity.domain.User;
import com.kodegravity.exr.identity.domain.UserRole;
import com.kodegravity.exr.identity.domain.WeakPasswordException;
import com.kodegravity.exr.identity.infra.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Registration: the only place in the service where a plaintext password exists,
 * and only for the length of one method call.
 *
 * <p><b>The order of events, and why it matters:</b>
 * <ol>
 *   <li>normalise the email (trim, lower-case for comparison);</li>
 *   <li>hash the password - the plaintext is never stored, never logged, and
 *       never passed further;</li>
 *   <li>persist the row. If the email is already taken, the UNIQUE index from V1
 *       rejects the INSERT; nothing here checks first, because a check here could
 *       be defeated by two simultaneous requests.</li>
 * </ol>
 *
 * <p><b>What is deliberately missing:</b> password rules (length, common-password
 * blocklist) and the duplicate-email error message. Both belong to the next step,
 * where they can be tried against a real request. Writing them now would be
 * guessing at an API that does not exist yet.
 */
@Service
public class UserAccountService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates an account and returns the stored user.
     *
     * <p>{@code saveAndFlush} rather than {@code save}, and that single word is
     * the whole difference between a working error message and the 500 you saw.
     * {@code save} only registers the object; the INSERT would travel at COMMIT,
     * which happens <em>after</em> this method returns - so a duplicate email
     * would be discovered outside the {@code try} below, and no exception handler
     * in this service could name it. The flush moves the INSERT forward on
     * purpose, so the unique index speaks while we can still translate what it
     * said.
     */
    @Transactional
    public User register(String email, String rawPassword) {
        // Checked BEFORE the hash: hashing first would spend 50-100 ms of CPU on a
        // password that is going to be refused, which is a free denial-of-service
        // lever for anyone who can post to this endpoint.
        List<String> violations = PasswordPolicy.violations(rawPassword);
        if (!violations.isEmpty()) {
            throw new WeakPasswordException(violations);
        }

        String normalisedEmail = email.trim();
        String hash = passwordEncoder.encode(rawPassword);

        User user = new User(UUID.randomUUID(), normalisedEmail, hash, UserRole.FREE_USER);

        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException clash) {
            // Which constraint failed is not known here, and does not need to be:
            // `users` has exactly one unique rule, the email. A second one would
            // require inspecting the message, and at that point the check belongs
            // in a dedicated query, not in a catch block.
            throw new EmailAlreadyUsedException(normalisedEmail);
        }
    }
}
