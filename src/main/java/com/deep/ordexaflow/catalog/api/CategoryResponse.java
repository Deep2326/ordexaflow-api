package com.deep.ordexaflow.catalog.api;

import java.time.Instant;
import java.util.UUID;

import com.deep.ordexaflow.catalog.domain.Category;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(), category.getName(), category.getSlug(), category.isActive(),
                category.getCreatedAt(), category.getUpdatedAt());
    }
}
