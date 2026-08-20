package com.deep.ordexaflow.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.deep.ordexaflow.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByActiveTrueOrderByNameAsc();
    Optional<Category> findByIdAndActiveTrue(UUID id);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
}
