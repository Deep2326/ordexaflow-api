package com.deep.ordexaflow.auth.api;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
    @Override
    public String toString() {
        return "LoginResponse[accessToken=[REDACTED], tokenType=%s, expiresIn=%d]"
                .formatted(tokenType, expiresIn);
    }
}
