package com.kodegravity.exr.identity.infra;

import com.kodegravity.exr.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Database access for {@code users}.
 *
 * <p><b>Why the query is written out instead of derived from the method name.</b>
 * The obvious Spring Data form is {@code findByEmailIgnoreCase(String email)},
 * which is shorter and reads better. It also compiles to
 * {@code upper(email) = upper(?)}, while the unique index from V1 is built on
 * {@code lower(email)}. Postgres cannot use an index built on one expression to
 * answer a predicate on a different one, so the login path silently degrades to
 * a sequential scan - correct results, table scan, on the hottest query in the
 * service. The explicit query keeps both sides {@code lower(...)} so the index
 * is usable, and it is the reason the migration's comment says "the query must
 * also use lower(email)".
 *
 * <p><b>What this repository deliberately does not do:</b> it does not enforce
 * uniqueness. That is the index's job, because an application check can be
 * bypassed by a concurrent request while the index cannot. Here, the second
 * insert simply fails - and the service above translates that failure into a
 * readable error.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds the account for a login attempt.
     *
     * <p>{@code Optional} rather than {@code null}: "no such user" is an ordinary
     * outcome of typing an email, not an exceptional one, and the caller must
     * decide what to do about it.
     */
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmail(@Param("email") String email);
}
