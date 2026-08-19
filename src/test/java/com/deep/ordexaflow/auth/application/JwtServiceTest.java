package com.deep.ordexaflow.auth.application;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import com.deep.ordexaflow.users.domain.Role;
import com.deep.ordexaflow.users.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtServiceTest {
    private static final byte[] SECRET =
            "test-only-secret-with-at-least-32-bytes".getBytes(UTF_8);

    @Test
    void issuesSignedAccessTokenWithIdentityAndAuthorizationClaims() {
        var secretKey = new SecretKeySpec(SECRET, "HmacSHA256");
        var encoder = NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();
        var decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        var jwtService = new JwtService(encoder, "http://localhost:8080", Duration.ofMinutes(15));
        var user = new User("Deep", "Patel", "deep@example.com", "password-hash", new Role("ROLE_USER"));

        var issuedToken = jwtService.issueAccessToken(user);
        var decodedToken = decoder.decode(issuedToken.value());

        assertThat(issuedToken.expiresInSeconds()).isEqualTo(900);
        assertThat(decodedToken.getIssuer().toString()).isEqualTo("http://localhost:8080");
        assertThat(decodedToken.getSubject()).isEqualTo("deep@example.com");
        assertThat(decodedToken.getClaimAsString("userId")).isEqualTo(user.getId().toString());
        assertThat(decodedToken.getClaimAsStringList("roles")).isEqualTo(List.of("ROLE_USER"));
        assertThat(decodedToken.getExpiresAt()).isAfter(decodedToken.getIssuedAt());
    }
}
