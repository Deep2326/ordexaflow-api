package com.deep.ordexaflow.users.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Set<String> roles,
        Instant createdAt) {
}
