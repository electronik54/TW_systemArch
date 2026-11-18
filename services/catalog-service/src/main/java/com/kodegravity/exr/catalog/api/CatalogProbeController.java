package com.kodegravity.exr.catalog.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Day 5.3's probe: two endpoints whose only job is to make the token rules visible.
 *
 * <p>It is not a product feature. Day 3 gives this service its real controllers
 * ({@code /api/events}, search, and so on); this one exists so that "the catalogue
 * trusts a token" can be <b>watched</b> in Postman instead of argued about in chat.
 *
 * <p><b>What is absent on purpose:</b> no token parsing, no signature check, no
 * {@code if (jwt == null)}. By the time this method runs, {@code SecurityConfig}'s
 * chain has already verified the signature, the issuer, the expiry and the audience,
 * and Spring has put the claims in an argument. A controller that re-checked them
 * would be a second implementation of the same rule, and the second one is the one
 * that drifts.
 */
@RestController
@RequestMapping("/api")
public class CatalogProbeController {

    /** Open, because the rule for it is {@code permitAll()} on {@code /api/public/**}. */
    @GetMapping("/public/ping")
    public Map<String, String> ping() {
        return Map.of("service", "catalog", "status", "up");
    }

    /**
     * Whoever reaches this line presented a valid token for this audience.
     *
     * <p>The values come from the token, not from the database - that is the whole
     * difference from the identity service: no query ran, and {@code identity} was not
     * contacted. What {@code catalog} knows about this user is exactly what the token
     * says, which is also the reason a token must stay short-lived.
     */
    @GetMapping("/catalog/whoami")
    public Map<String, Object> whoami(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "sub", jwt.getSubject(),
                "email", String.valueOf(jwt.getClaimAsString("email")),
                "roles", String.valueOf(jwt.getClaimAsStringList("roles")),
                "audience", jwt.getAudience(),
                "issuer", String.valueOf(jwt.getClaimAsString("iss")));
    }

    /**
     * Day 5.4: <b>authorization</b>, as opposed to authentication.
     *
     * <p>Every request that reaches this line already carried a valid token - the chain
     * proved that. What it has not proved until this annotation runs is that the
     * <em>role</em> inside that token may do this. Authentication and authorization are
     * two separate decisions, and keeping them separate is what lets the first one be
     * cheap and stateless.
     *
     * <p><b>Why the refusal is 403 and not 401.</b> {@code 401} means "I do not know
     * who you are" and invites a retry with credentials. {@code 403} means "I know
     * exactly who you are, and you may not" - retrying changes nothing. Returning 401
     * here would send a client into a pointless login loop.
     *
     * <p><b>Where the check happens.</b> Not in an {@code if} inside the method: an
     * interceptor calls this expression <em>before</em> the body runs, so a refused
     * caller never executes a line of it. The same list of authorities came from the
     * token's {@code roles} claim, which means {@code catalog} decides on data that
     * {@code identity} signed - no call, no shared session.
     */
    @GetMapping("/catalog/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminPing(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("admin", "yes", "sub", jwt.getSubject());
    }
}
