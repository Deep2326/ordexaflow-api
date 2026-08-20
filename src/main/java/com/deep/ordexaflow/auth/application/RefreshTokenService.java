package com.deep.ordexaflow.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.deep.ordexaflow.auth.api.LoginResponse;
import com.deep.ordexaflow.auth.domain.RefreshToken;
import com.deep.ordexaflow.auth.infrastructure.RefreshTokenRepository;
import com.deep.ordexaflow.users.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator tokenGenerator;
    private final JwtService jwtService;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator tokenGenerator,
            JwtService jwtService,
            Clock clock,
            @Value("${ordexaflow.security.jwt.refresh-token-ttl}") Duration refreshTokenTtl) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.jwtService = jwtService;
        this.clock = clock;
        if (refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
            throw new IllegalArgumentException("Refresh token TTL must be positive");
        }
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        return issue(user, UUID.randomUUID(), clock.instant());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public LoginResponse rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken currentToken = refreshTokenRepository.findByTokenHashForUpdate(tokenGenerator.hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (currentToken.isRevoked()) {
            if (currentToken.wasRotated()) {
                refreshTokenRepository.revokeActiveFamily(currentToken.getFamilyId(), now);
            }
            throw new InvalidRefreshTokenException();
        }
        if (currentToken.isExpired(now)) {
            currentToken.revoke(now);
            throw new InvalidRefreshTokenException();
        }
        if (!currentToken.getUser().isEnabled()) {
            refreshTokenRepository.revokeActiveFamily(currentToken.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }

        IssuedRefreshToken replacement = issue(currentToken.getUser(), currentToken.getFamilyId(), now);
        currentToken.rotateTo(replacement.id(), now);
        JwtService.IssuedToken accessToken = jwtService.issueAccessToken(currentToken.getUser());
        return new LoginResponse(
                accessToken.value(), replacement.value(), "Bearer",
                accessToken.expiresInSeconds(), replacement.expiresInSeconds());
    }

    @Transactional
    public void revoke(String rawToken) {
        Instant now = clock.instant();
        refreshTokenRepository.findByTokenHashForUpdate(tokenGenerator.hash(rawToken)).ifPresent(token -> {
            if (token.wasRotated()) {
                refreshTokenRepository.revokeActiveFamily(token.getFamilyId(), now);
            } else {
                token.revoke(now);
            }
        });
    }

    private IssuedRefreshToken issue(User user, UUID familyId, Instant issuedAt) {
        RefreshTokenGenerator.GeneratedToken generatedToken = tokenGenerator.generate();
        Instant expiresAt = issuedAt.plus(refreshTokenTtl);
        RefreshToken token = new RefreshToken(user, familyId, generatedToken.hash(), issuedAt, expiresAt);
        refreshTokenRepository.save(token);
        return new IssuedRefreshToken(token.getId(), generatedToken.value(), refreshTokenTtl.toSeconds());
    }

    public record IssuedRefreshToken(UUID id, String value, long expiresInSeconds) {
        @Override
        public String toString() {
            return "IssuedRefreshToken[id=%s, value=[REDACTED], expiresInSeconds=%d]"
                    .formatted(id, expiresInSeconds);
        }
    }
}
