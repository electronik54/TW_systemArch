package com.kodegravity.exr.identity.application;

import com.kodegravity.exr.identity.domain.UserRole;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mints the token that replaces the password on every later request (Day 5.1).
 *
 * <p><b>What a JWT is.</b> Three pieces joined by dots: a <em>header</em> naming the
 * algorithm, a <em>payload</em> of claims, and a <em>signature</em> over the first two.
 * Header and payload are base64url, which is <b>encoding, not encryption</b> — anyone
 * holding the token can read every claim. The signature is what nobody can forge
 * without the key, so a service can trust the payload without asking us.
 *
 * <p><b>Why each claim is here.</b>
 * <ul>
 *   <li>{@code sub} — who: the user's id. Never the email, because an email can be
 *       changed and a token minted before the change would keep pointing at it.</li>
 *   <li>{@code roles} — what they may do. One {@code ROLE_*} string, because that is
 *       the shape Spring's authorization rules read.</li>
 *   <li>{@code iss} — who minted it. A service refuses tokens from anywhere else.</li>
 *   <li>{@code aud} — who it is for. This single claim is the heart of inter-service
 *       security (7.6): a token for {@code exr-api} is not a token for the payment
 *       service, so a stolen one cannot be replayed against another audience.</li>
 *   <li>{@code exp} — when it stops working. Short, because a stolen token cannot be
 *       recalled; its only defence is that it dies soon.</li>
 *   <li>{@code jti} — a unique id for this token, so it <em>can</em> be denied
 *       individually (5.5) and so two tokens for one user are never identical.</li>
 * </ul>
 *
 * <p><b>Why the encoder is injected.</b> So this class has no opinion about the
 * signing algorithm or the key. Today the key is a shared secret (HMAC, HS256); the
 * day a second service only knows our public key, the encoder changes and this class
 * does not.
 */
public class TokenService {

    /** Short on purpose: the only recall mechanism a stateless token has is expiry. */
    public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;

    public TokenService(JwtEncoder encoder, String issuer, String audience) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
    }

    /**
     * Builds the claims and signs them. Returns the compact string a client sends back
     * as {@code Authorization: Bearer <token>}.
     */
    public String issueAccessToken(UUID userId, String email, UserRole role) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_TTL))
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("roles", List.of("ROLE_" + role.name()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return sign(header, claims);
    }

    /** Five minutes: a service token is used immediately and never cached for long. */
    public static final Duration SERVICE_TOKEN_TTL = Duration.ofMinutes(5);

    /**
     * A token that identifies a <b>service</b>, not a person (Day 7.6).
     *
     * <p><b>Why this exists.</b> The user's token answers "on whose behalf", and it is
     * the right thing to forward when a service acts for a user. It cannot answer "who
     * is calling" for work that belongs to nobody - a nightly purge, a webhook relay,
     * a call between two services. Those needs an identity of their own, or every such
     * call would have to be made as some arbitrary human.
     *
     * <p><b>What it deliberately carries:</b> {@code sub} = the service's name,
     * {@code roles} = {@code ROLE_SERVICE} and nothing more. Least privilege in one
     * line: a service token must not inherit a person's rights, and a receiver that
     * asks for {@code hasRole('ADMIN')} will happily refuse it.
     *
     * <p><b>How it is obtained</b> is the caller's problem, not this method's: the
     * service presents a shared key and receives a token. That is the
     * client-credentials shape of OAuth2, with the two honest caveats written at the
     * endpoint - the key is symmetric, and rotation is manual until a real
     * authorization server exists.
     */
    public String issueServiceToken(String serviceName) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(serviceName)
                .issuedAt(now)
                .expiresAt(now.plus(SERVICE_TOKEN_TTL))
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of("ROLE_SERVICE"))
                .build();

        return sign(JwsHeader.with(MacAlgorithm.HS256).build(), claims);
    }

    /** One place that signs, so the algorithm choice is made once. */
    private String sign(JwsHeader header, JwtClaimsSet claims) {
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
