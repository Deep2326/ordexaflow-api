package com.deep.ordexaflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.deep.ordexaflow.catalog.api.CategoryRequest;
import com.deep.ordexaflow.catalog.api.CreateProductRequest;
import com.deep.ordexaflow.catalog.domain.Category;
import com.deep.ordexaflow.catalog.infrastructure.CategoryRepository;
import com.deep.ordexaflow.catalog.infrastructure.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(CatalogAuthorizationTest.MethodSecurityConfiguration.class)
class CatalogAuthorizationTest {
    private static final Instant NOW = Instant.parse("2026-08-20T20:00:00Z");

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private Clock clock;

    @Test
    @WithMockUser(roles = "USER")
    void deniesCustomerAtProductServiceBoundary() {
        var request = new CreateProductRequest(
                "LAPTOP-001", "Developer Laptop", "Portable workstation",
                new BigDecimal("1299.00"), "USD", UUID.randomUUID(), 8);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void permitsAdminAtCategoryServiceBoundary() {
        when(clock.instant()).thenReturn(NOW);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = categoryService.create(new CategoryRequest("Laptops", "laptops", true));

        assertThat(response.slug()).isEqualTo("laptops");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @Import({CategoryService.class, ProductService.class})
    static class MethodSecurityConfiguration {
    }
}
