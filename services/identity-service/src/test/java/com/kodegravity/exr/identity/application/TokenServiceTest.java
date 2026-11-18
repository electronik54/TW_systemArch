package com.kodegravity.exr.identity.application;

import com.kodegravity.exr.identity.domain.UserRole;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Day 5.1: a token is minted, and the claims can be read back without any secret.
 *
 * <p><b>Why a plain JUnit test.</b> No Spring, no database, no Docker: an encoder, a
 * decoder and a key are enough. The whole point of a JWT is that the payload travels
 * in the clear, so reading it back needs nothing but base64url — the key is only
 * needed to check the signature.
 *
 * <p><b>The two claims that carry the mechanism</b>, as opposed to the decoration:
 * <ol>
 *   <li>{@code aud} — a token minted for one audience is refused by another, which is
 *       what makes it usable <em>between</em> services (7.6);</li>
 *   <li>the signature — change one character of the payload and the token stops being
 *       valid, because the signature was computed over the original bytes.</li>
 * </ol>
 */
class TokenServiceTest {

    /** 32+ bytes: HS256 requires a 256-bit key, and a shorter one is refused outright. */
    private static final SecretKey KEY = new SecretKeySpec(
            "exr-dev-only-secret-32-bytes-minimum!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private static final String ISSUER = "exr-identity";
    private static final String AUDIENCE = "exr-api";

    private final TokenService tokenService = new TokenService(
            new NimbusJwtEncoder(new ImmutableSecret<>(KEY)), ISSUER, AUDIENCE);

    private final JwtDecoder decoder =
            NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();

    @Test
    @DisplayName("the token has three parts and carries who, what, for whom and until when")
    void tokenCarriesTheClaims() {
        UUID userId = UUID.randomUUID();

        String token = tokenService.issueAccessToken(userId, "alice@example.com", UserRole.FREE_USER);

        // header . payload . signature
        assertThat(token.split("\\.")).hasSize(3);

        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("email")).isEqualTo("alice@example.com");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_FREE_USER");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(jwt.getAudience()).containsExactly(AUDIENCE);
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("changing the payload invalidates the signature, so the token stops decoding")
    void tamperedPayloadIsRejected() {
        String token = tokenService.issueAccessToken(UUID.randomUUID(), "bob@example.com", UserRole.PAID_USER);
        String[] parts = token.split("\\.");

        // Flip one character inside the payload, leaving it valid base64url so the
        // failure cannot be blamed on a decoding error.
        char[] payload = parts[1].toCharArray();
        payload[5] = payload[5] == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];

        assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(JwtException.class);
    }
}
