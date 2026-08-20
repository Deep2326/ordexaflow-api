package com.deep.ordexaflow.catalog.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.deep.ordexaflow.catalog.api.CategoryRequest;
import com.deep.ordexaflow.catalog.api.CategoryResponse;
import com.deep.ordexaflow.catalog.domain.Category;
import com.deep.ordexaflow.catalog.infrastructure.CategoryRepository;
import com.deep.ordexaflow.common.exception.DuplicateResourceException;
import com.deep.ordexaflow.common.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public CategoryService(CategoryRepository categoryRepository, Clock clock) {
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse create(CategoryRequest request) {
        String slug = normalizeSlug(request.slug());
        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Category", "slug", slug);
        }
        Instant now = clock.instant();
        Category category = new Category(request.name().trim(), slug, now);
        if (!request.active()) {
            category.update(category.getName(), slug, false, now);
        }
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(UUID categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        String slug = normalizeSlug(request.slug());
        if (categoryRepository.existsBySlugAndIdNot(slug, categoryId)) {
            throw new DuplicateResourceException("Category", "slug", slug);
        }
        category.update(request.name().trim(), slug, request.active(), clock.instant());
        return CategoryResponse.from(category);
    }

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase(Locale.ROOT);
    }
}
