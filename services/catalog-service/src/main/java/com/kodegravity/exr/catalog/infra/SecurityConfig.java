package com.kodegravity.exr.catalog.infra;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Day 5.3: this service checks tokens itself.
 *
 * <p><b>The point of the whole exercise.</b> There is no call to {@code identity} on
 * the way in. The token arrives, its signature is recomputed here with the shared
 * secret, and the claims are trusted because they could not have been forged. That is
 * what makes a stateless token worth having: {@code identity} can be down and
 * {@code catalog} still knows who is calling. The cost is the one already recorded in
 * the identity service - with HMAC, every verifier also holds the power to
 * <em>mint</em>.
 *
 * <p><b>Three checks, and only the third is about the token's owner:</b>
 * <ol>
 *   <li><b>signature</b> - is this ours (the key), and is it unedited;</li>
 *   <li><b>issuer, expiry, not-before</b> - {@link JwtValidators#createDefaultWithIssuer}
 *       bundles those, so an expired token or a foreign issuer is refused before any
 *       claim is read;</li>
 *   <li><b>audience</b> - was this token minted for <em>us</em>? A perfectly signed
 *       token for {@code exr-payment} is refused here, which is what stops one stolen
 *       token being replayed against every service in the estate.</li>
 * </ol>
 *
 * <p><b>What is knowingly missing.</b> A refusal currently produces Spring's default
 * 401: no body, no correlation id - the opposite of what {@code
 * RestAuthenticationEntryPoint} does in the identity service. The fix is to move that
 * class into {@code exr-common} and use it here too; it is scheduled with the gateway
 * work (Day 7), where the refusal shape becomes a platform-wide decision.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * The rules, first match wins.
     *
     * <p>{@code .oauth2ResourceServer(...)} installs the bearer-token filter: a request
     * carrying {@code Authorization: Bearer <token>} is authenticated there, long
     * before any controller runs. No controller in this service ever sees a token.
     */
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                // Stateless, no cookies, so the CSRF attack has no vehicle - same
                // reasoning, and the same condition that would reverse it, as in
                // the identity service.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Only the health-style probe is open. Every real catalogue
                        // endpoint added on Day 3 falls under "authenticated" by
                        // default, which is the safe direction for a default to fail.
                        .requestMatchers("/api/public/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> {
                }))
                // No login page and no Basic: this service is not a front door.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Turns a token back into claims, or refuses it.
     *
     * <p>The audience check is written by hand rather than with
     * {@code JwtClaimValidator} because {@link Jwt#getAudience()} is the reliable view
     * of that claim: the wire format allows a bare string or an array, and this reads
     * whichever arrived.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${exr.security.jwt.secret}") String secret,
                          @Value("${exr.security.jwt.issuer}") String issuer,
                          @Value("${exr.security.jwt.audience}") String audience) {

        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> audienceCheck = token ->
                token.getAudience().contains(audience)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                                "invalid_token",
                                "This token was not minted for audience " + audience,
                                null));

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                audienceCheck));

        return decoder;
    }
}
