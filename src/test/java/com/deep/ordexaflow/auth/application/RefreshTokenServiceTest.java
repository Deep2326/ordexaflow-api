package com.deep.ordexaflow.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.deep.ordexaflow.auth.domain.RefreshToken;
import com.deep.ordexaflow.auth.infrastructure.RefreshTokenRepository;
import com.deep.ordexaflow.users.domain.Role;
import com.deep.ordexaflow.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefreshTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");
    private static final Duration REFRESH_TTL = Duration.ofDays(30);

    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenGenerator tokenGenerator;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;
    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        tokenGenerator = mock(RefreshTokenGenerator.class);
        jwtService = mock(JwtService.class);
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository, tokenGenerator, jwtService,
                Clock.fixed(NOW, ZoneOffset.UTC), REFRESH_TTL);
        user = new User("Deep", "Patel", "deep@example.com", "password-hash", new Role("ROLE_USER"));
    }

    @Test
    void rejectsNonPositiveRefreshTokenTtl() {
        assertThatThrownBy(() -> new RefreshTokenService(
                refreshTokenRepository, tokenGenerator, jwtService,
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token TTL must be positive");
    }

    @Test
    void issuesOpaqueRefreshTokenButPersistsOnlyItsHash() {
        when(tokenGenerator.generate()).thenReturn(
                new RefreshTokenGenerator.GeneratedToken("raw-refresh-token", "hashed-refresh-token"));

        var issuedToken = refreshTokenService.issue(user);

        ArgumentCaptor<RefreshToken> persistedToken = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(persistedToken.capture());
        assertThat(persistedToken.getValue().getTokenHash()).isEqualTo("hashed-refresh-token");
        assertThat(persistedToken.getValue().getTokenHash()).doesNotContain("raw-refresh-token");
        assertThat(persistedToken.getValue().getUser()).isSameAs(user);
        assertThat(persistedToken.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(persistedToken.getValue().getExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
        assertThat(issuedToken.value()).isEqualTo("raw-refresh-token");
        assertThat(issuedToken.expiresInSeconds()).isEqualTo(REFRESH_TTL.toSeconds());
    }

    @Test
    void rotatesActiveTokenAndIssuesNewAccessToken() {
        UUID familyId = UUID.randomUUID();
        RefreshToken currentToken = new RefreshToken(
                user, familyId, "current-hash", NOW.minusSeconds(60), NOW.plus(REFRESH_TTL));
        when(tokenGenerator.hash("current-raw-token")).thenReturn("current-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("current-hash"))
                .thenReturn(Optional.of(currentToken));
        when(tokenGenerator.generate()).thenReturn(
                new RefreshTokenGenerator.GeneratedToken("replacement-raw-token", "replacement-hash"));
        when(jwtService.issueAccessToken(user)).thenReturn(new JwtService.IssuedToken("access-token", 900));

        var response = refreshTokenService.rotate("current-raw-token");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("replacement-raw-token");
        assertThat(currentToken.isRevoked()).isTrue();
        assertThat(currentToken.wasRotated()).isTrue();
        ArgumentCaptor<RefreshToken> replacement = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(replacement.capture());
        assertThat(replacement.getValue().getFamilyId()).isEqualTo(familyId);
        assertThat(currentToken.getReplacedByTokenId()).isEqualTo(replacement.getValue().getId());
    }

    @Test
    void revokesEntireFamilyWhenRotatedTokenIsReused() {
        UUID familyId = UUID.randomUUID();
        RefreshToken reusedToken = new RefreshToken(
                user, familyId, "reused-hash", NOW.minusSeconds(120), NOW.plus(REFRESH_TTL));
        reusedToken.rotateTo(UUID.randomUUID(), NOW.minusSeconds(60));
        when(tokenGenerator.hash("reused-raw-token")).thenReturn("reused-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("reused-hash"))
                .thenReturn(Optional.of(reusedToken));

        assertThatThrownBy(() -> refreshTokenService.rotate("reused-raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid or expired");

        verify(refreshTokenRepository).revokeActiveFamily(familyId, NOW);
        verify(jwtService, never()).issueAccessToken(any());
    }

    @Test
    void rejectsAndRevokesExpiredToken() {
        RefreshToken expiredToken = new RefreshToken(
                user, UUID.randomUUID(), "expired-hash", NOW.minus(REFRESH_TTL), NOW.minusSeconds(1));
        when(tokenGenerator.hash("expired-raw-token")).thenReturn("expired-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("expired-hash"))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.rotate("expired-raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(expiredToken.getRevokedAt()).isEqualTo(NOW);
        verify(jwtService, never()).issueAccessToken(any());
    }

    @Test
    void rejectsUnknownTokenWithoutRevealingWhetherItExists() {
        when(tokenGenerator.hash("unknown-token")).thenReturn("unknown-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("unknown-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid or expired");
    }

    @Test
    void logoutRevokesActiveTokenAndIsIdempotentForUnknownToken() {
        RefreshToken activeToken = new RefreshToken(
                user, UUID.randomUUID(), "active-hash", NOW.minusSeconds(60), NOW.plus(REFRESH_TTL));
        when(tokenGenerator.hash("active-token")).thenReturn("active-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("active-hash"))
                .thenReturn(Optional.of(activeToken));

        refreshTokenService.revoke("active-token");

        assertThat(activeToken.getRevokedAt()).isEqualTo(NOW);

        when(tokenGenerator.hash("unknown-token")).thenReturn("unknown-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("unknown-hash")).thenReturn(Optional.empty());
        refreshTokenService.revoke("unknown-token");
    }
}
