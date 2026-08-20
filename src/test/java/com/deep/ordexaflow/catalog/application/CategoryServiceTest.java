package com.deep.ordexaflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.deep.ordexaflow.catalog.api.CategoryRequest;
import com.deep.ordexaflow.catalog.domain.Category;
import com.deep.ordexaflow.catalog.infrastructure.CategoryRepository;
import com.deep.ordexaflow.common.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-20T20:00:00Z");

    private CategoryRepository categoryRepository;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        categoryService = new CategoryService(
                categoryRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void listsOnlyActiveCategoriesInRepositoryOrder() {
        Category laptops = new Category("Laptops", "laptops", NOW);
        Category phones = new Category("Phones", "phones", NOW);
        when(categoryRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(laptops, phones));

        var categories = categoryService.listActive();

        assertThat(categories).extracting("slug").containsExactly("laptops", "phones");
    }

    @Test
    void createsNormalizedCategory() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = categoryService.create(new CategoryRequest("  Laptops  ", "laptops", true));

        assertThat(response.name()).isEqualTo("Laptops");
        assertThat(response.slug()).isEqualTo("laptops");
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsDuplicateSlug() {
        when(categoryRepository.existsBySlug("laptops")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryRequest("Laptops", "laptops", true)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Category already exists with slug: laptops");
    }
}
