package com.deep.ordexaflow.catalog.infrastructure;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.deep.ordexaflow.catalog.domain.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {
    private ProductSpecifications() {
    }

    public static Specification<Product> publicCatalog(
            String categorySlug, BigDecimal minPrice, BigDecimal maxPrice, String query) {
        List<Specification<Product>> specifications = new ArrayList<>();
        specifications.add((root, criteriaQuery, builder) -> builder.isTrue(root.get("active")));
        specifications.add((root, criteriaQuery, builder) -> builder.isTrue(root.get("category").get("active")));

        if (categorySlug != null && !categorySlug.isBlank()) {
            String normalizedSlug = categorySlug.trim().toLowerCase(Locale.ROOT);
            specifications.add((root, criteriaQuery, builder) ->
                    builder.equal(root.get("category").get("slug"), normalizedSlug));
        }
        if (minPrice != null) {
            specifications.add((root, criteriaQuery, builder) ->
                    builder.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            specifications.add((root, criteriaQuery, builder) ->
                    builder.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            specifications.add((root, criteriaQuery, builder) -> builder.or(
                    builder.like(builder.lower(root.get("name")), pattern),
                    builder.like(builder.lower(root.get("sku")), pattern),
                    builder.like(builder.lower(root.get("description")), pattern)));
        }
        return Specification.allOf(specifications);
    }
}
