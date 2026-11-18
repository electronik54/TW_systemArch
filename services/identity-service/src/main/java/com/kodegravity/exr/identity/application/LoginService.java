package com.kodegravity.exr.identity.application;

import com.kodegravity.exr.identity.domain.User;
import com.kodegravity.exr.identity.infra.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Day 5.2: the login that hands out a token instead of asking for the password again.
 *
 * <p><b>The one design decision in this class:</b> it does not check the password. It
 * asks {@link AuthenticationManager} to, and that manager runs the very
 * {@code DaoAuthenticationProvider} the HTTP Basic filter runs. So every rule built on
 * Day 4.3 still applies - a suspended account is refused, a locked one is refused
 * before the password is compared, a wrong password increments
 * {@code failed_attempts}, and the fifth one writes {@code locked_until}. Reusing the
 * door is why none of that had to be repeated here; re-implementing `matches(...)`
 * would have quietly dropped all of it.
 *
 * <p><b>Why the row is read a second time.</b> The authenticated principal carries the
 * username and the authorities Spring needs, but not our {@code users.id} - and the
 * token's {@code sub} must be the id, because an email can be changed. One indexed
 * lookup by email is a cheap price for a subject that survives an email change.
 *
 * <p><b>Why there is no {@code @Transactional} here.</b> A login is not a unit of work
 * we own: the failed-attempt write belongs to {@code LoginAttemptService}, which opens
 * its own transaction so the count survives. Wrapping this method in a transaction
 * would only make two writes that must be independent look atomic.
 */
@Service
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final TokenService tokens;

    public LoginService(AuthenticationManager authenticationManager,
                        UserRepository users,
                        TokenService tokens) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.tokens = tokens;
    }

    /**
     * @throws org.springframework.security.core.AuthenticationException when the
     *         credentials are wrong, the account is locked, or it is suspended - the
     *         caller turns all three into one 401 on purpose.
     */
    public IssuedToken login(String email, String rawPassword) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, rawPassword));

        User user = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "authenticated but the row is gone: " + authentication.getName()));

        String token = tokens.issueAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new IssuedToken(token, TokenService.ACCESS_TOKEN_TTL.toSeconds());
    }

    /** What the client receives. The token, its type, and how long it lasts. */
    public record IssuedToken(String accessToken, long expiresInSeconds) {
    }

    /**
     * Day 7.6, the missing half: a token that says which <b>service</b> is calling.
     *
     * <p>No password, no user row, no {@code AuthenticationManager}: the caller has
     * already proved itself with the shared key at the controller, and this only mints
     * the result. Keeping the two apart matters - a service token that also required a
     * password would mean storing a password in every service.
     */
    public IssuedToken issueServiceToken(String serviceName) {
        return new IssuedToken(
                tokens.issueServiceToken(serviceName),
                TokenService.SERVICE_TOKEN_TTL.toSeconds());
    }
}
