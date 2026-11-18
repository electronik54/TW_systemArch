package com.kodegravity.exr.identity.api;

import com.kodegravity.exr.identity.application.LoginService;
import com.kodegravity.exr.identity.application.TokenService;
import com.kodegravity.exr.identity.application.UserAccountService;
import com.kodegravity.exr.identity.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * The registration door. Deliberately thin: it translates JSON, calls one
 * service method, and maps the result to a status code. No hashing, no
 * validation logic, no repository.
 *
 * <p><b>Why the response is a record and not the {@link User} entity.</b> An
 * entity is a live database object with a version counter and a password hash on
 * it. Serialising it would publish the hash and tie the API's shape to the table's
 * shape, so a column rename would become a breaking API change.
 *
 * <p><b>What it returns:</b> the id, the email as stored, and the role - enough
 * for a client to show "welcome", and nothing that could be replayed or cracked.
 * The status is {@code 201 Created} with a {@code Location} header, because the
 * request created a resource and a client may want to fetch it later.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAccountService accounts;
    private final LoginService logins;
    /** Shared key a service presents to get a token of its own (7.6). */
    private final String serviceKey;

    public AuthController(UserAccountService accounts,
                          LoginService logins,
                          @Value("${exr.identity.service-key}") String serviceKey) {
        this.accounts = accounts;
        this.logins = logins;
        this.serviceKey = serviceKey;
    }

    @PostMapping("/register")
    public ResponseEntity<Registered> register(@RequestBody RegisterRequest request) {
        User user = accounts.register(request.email(), request.password());

        return ResponseEntity
                .created(URI.create("/api/users/" + user.getId()))
                .body(new Registered(user.getId(), user.getEmail(), user.getRole().name()));
    }

    /** What a client may send. No id, no role: the server decides those. */
    public record RegisterRequest(String email, String password) {
    }

    /**
     * Day 5.2: the door that returns a token.
     *
     * <p>{@code 200}, not {@code 201}: a login creates nothing on the server, so
     * "created" would be a lie. A refusal is {@code 401} from
     * {@code ApiExceptionHandler}, and it deliberately does not say whether the email
     * exists, the password was wrong, or the account is locked - that distinction is a
     * gift to anyone probing for valid addresses.
     *
     * <p>The token is returned in the <b>body</b>, not in a header, because the caller
     * is expected to store it and send it later as {@code Authorization: Bearer ...}.
     */
    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        LoginService.IssuedToken issued = logins.login(request.email(), request.password());

        return new TokenResponse(issued.accessToken(), "Bearer", issued.expiresInSeconds());
    }

    public record LoginRequest(String email, String password) {
    }

    /** {@code expiresInSeconds} lets a client refresh before the token dies, not after. */
    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
    }

    /** What a client gets back. No hash, ever. */
    public record Registered(UUID id, String email, String role) {
    }

    /**
     * Day 7.6, the second half: <b>a service proves it is itself</b>, then receives a
     * token that says so.
     *
     * <p>This is the client-credentials shape of OAuth2 in its smallest honest form:
     * one shared key in, one short-lived token out. No user is involved, which is the
     * point - a nightly purge or a webhook relay acts for nobody.
     *
     * <p><b>Two weaknesses stated rather than hidden.</b> (1) One symmetric key serves
     * every service, so it proves "some trusted client", not "catalog specifically"; a
     * per-service key table is the fix. (2) Rotation is manual - changing
     * {@code exr.identity.service-key} invalidates every caller at once, which is why a
     * real system moves this to an authorization server with per-client credentials and
     * overlap windows.
     *
     * <p><b>Why a wrong key is a {@code BadCredentialsException}.</b> It maps to the
     * same {@code 401 invalid-credentials} body as a failed login, so a caller guessing
     * keys learns nothing about what went wrong.
     */
    @PostMapping("/service-token")
    public TokenResponse serviceToken(@RequestHeader("X-Service-Key") String presentedKey,
                                      @RequestBody ServiceTokenRequest request) {
        if (!serviceKey.equals(presentedKey)) {
            throw new BadCredentialsException("The service key was rejected");
        }

        LoginService.IssuedToken issued = logins.issueServiceToken(request.serviceName());
        return new TokenResponse(issued.accessToken(), "Bearer", issued.expiresInSeconds());
    }

    /** Which service is asking. A registry would validate this; a shared key cannot. */
    public record ServiceTokenRequest(String serviceName) {
    }
}
