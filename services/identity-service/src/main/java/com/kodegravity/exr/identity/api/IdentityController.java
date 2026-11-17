package com.kodegravity.exr.identity.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Two endpoints, and nothing else, so the security policy can be SEEN rather
 * than believed.
 *
 * <p>They exist because a security rule that nobody has requested is a rule
 * nobody can check. {@code /api/public/ping} must answer without credentials;
 * {@code /api/me} must not.
 *
 * <p>What is deliberately absent: any authentication code. There is no
 * {@code if (user == null)} here, no token parsing, no filter. The controller
 * is only reached when the chain has already decided the caller is allowed -
 * that is the whole point of putting the rule in the filter chain instead of in
 * every method.
 */
@RestController
@RequestMapping("/api")
public class IdentityController {

    /** Reachable by anyone - the rule for it is {@code permitAll()}. */
    @GetMapping("/public/ping")
    public Map<String, String> ping() {
        return Map.of("status", "up");
    }

    /**
     * Whoever reaches this line is already authenticated, so the principal is
     * available for free. Its name is what the filter chain resolved: with Basic
     * auth today that is the username, and from Day 5 it will be a claim read out
     * of a token.
     */
    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of(
                "name", authentication.getName(),
                "authorities", authentication.getAuthorities().toString());
    }
}
