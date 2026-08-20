package com.deep.ordexaflow.auth.api;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn) {

    @Override
    public String toString() {
        return "LoginResponse[accessToken=[REDACTED], refreshToken=[REDACTED], "
                + "tokenType=%s, expiresIn=%d, refreshExpiresIn=%d]"
                        .formatted(tokenType, expiresIn, refreshExpiresIn);
    }
}
