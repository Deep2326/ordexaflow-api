package com.deep.ordexaflow.catalog.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.deep.ordexaflow.catalog.domain.Product;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int availableQuantity,
        boolean active,
        CategorySummary category,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(), product.getSku(), product.getName(), product.getDescription(),
                product.getPrice(), product.getCurrency(), product.getInventory().getQuantity(),
                product.isActive(), CategorySummary.from(product),
                product.getCreatedAt(), product.getUpdatedAt());
    }

    public record CategorySummary(UUID id, String name, String slug) {
        static CategorySummary from(Product product) {
            return new CategorySummary(
                    product.getCategory().getId(), product.getCategory().getName(),
                    product.getCategory().getSlug());
        }
    }
}
