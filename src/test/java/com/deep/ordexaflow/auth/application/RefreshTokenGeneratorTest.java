package com.deep.ordexaflow.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenGeneratorTest {
    private final RefreshTokenGenerator tokenGenerator = new RefreshTokenGenerator();

    @Test
    void generatesUniqueOpaqueTokensAndSha256Hashes() {
        var first = tokenGenerator.generate();
        var second = tokenGenerator.generate();

        assertThat(first.value()).hasSize(43).doesNotContain("=");
        assertThat(first.hash()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(first.hash()).isEqualTo(tokenGenerator.hash(first.value()));
        assertThat(second.value()).isNotEqualTo(first.value());
        assertThat(second.hash()).isNotEqualTo(first.hash());
    }

    @Test
    void redactsGeneratedTokenFromStringRepresentation() {
        var token = tokenGenerator.generate();

        assertThat(token.toString()).contains("[REDACTED]")
                .doesNotContain(token.value(), token.hash());
    }
}
