package com.deep.ordexaflow.auth.api;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
