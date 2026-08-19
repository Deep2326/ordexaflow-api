package com.deep.ordexaflow.auth.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserRegistrationResponse(
        UUID id, String firstName, String lastName, String email, Set<String> roles, Instant createdAt) {
}
