package com.kodegravity.exr.identity.infra;

import com.kodegravity.exr.identity.domain.User;
import com.kodegravity.exr.identity.domain.UserStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * The bridge between our {@code users} table and Spring Security: it answers the
 * single question the framework asks — "who is this username, and may they log
 * in?".
 *
 * <p><b>What changes the moment this bean exists.</b> Boot's
 * {@code UserDetailsServiceAutoConfiguration} backs off, so the generated
 * {@code user} account and the random password in the startup log disappear. Basic
 * authentication now succeeds only for a real row in our table, checked against
 * the stored BCrypt hash - and {@code /api/me} finally reports a real identity and
 * a real authority instead of {@code []}.
 *
 * <p><b>Why nothing else had to be written.</b> A {@code UserDetailsService} plus
 * a {@code PasswordEncoder} bean are exactly what
 * {@code DaoAuthenticationProvider} needs; Spring wires them together. Writing our
 * own {@code AuthenticationProvider} here would be replacing a working component
 * to feel productive.
 *
 * <p><b>Two security details that are handled for us, and must not be undone:</b>
 * <ol>
 *   <li><b>No user enumeration.</b> Throwing {@link UsernameNotFoundException} does
 *       not reach the client: {@code DaoAuthenticationProvider} converts it to the
 *       same {@code BadCredentialsException} a wrong password produces, so both
 *       cases are one 401.</li>
 *   <li><b>No timing oracle.</b> "No such user" would otherwise answer instantly
 *       while a wrong password costs a BCrypt check, and that difference leaks the
 *       same information. The provider performs a dummy hash to equalise the
 *       timing.</li>
 * </ol>
 *
 * <p><b>Why {@code @Transactional(readOnly = true)}:</b> this method is called by
 * the framework, at a moment when no transaction may be open. The read must run in
 * one, or lazy loading and the connection lifecycle become unpredictable.
 */
@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public DbUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = users.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                // The ROLE_ prefix is Spring's convention: `hasRole("FREE_USER")` looks
                // for the authority named ROLE_FREE_USER.
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                // Account STATE is separate from the password. All three end the same
                // way (401), but the reason stays distinct in the model so an admin can
                // tell a temporary lockout from a suspension. The third condition is
                // Day 4.3's lockout: the deadline lives in `locked_until`, so it expires
                // by itself and needs no background job to clear it.
                .disabled(user.getStatus() == UserStatus.SUSPENDED)
                .accountLocked(user.getStatus() == UserStatus.LOCKED
                        || user.isTemporarilyLocked(Instant.now()))
                .build();
    }
}
