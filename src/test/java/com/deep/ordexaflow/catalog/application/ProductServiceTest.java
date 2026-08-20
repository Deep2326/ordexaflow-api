package com.deep.ordexaflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.deep.ordexaflow.catalog.api.CreateProductRequest;
import com.deep.ordexaflow.catalog.api.InventoryUpdateRequest;
import com.deep.ordexaflow.catalog.domain.Category;
import com.deep.ordexaflow.catalog.domain.Product;
import com.deep.ordexaflow.catalog.infrastructure.CategoryRepository;
import com.deep.ordexaflow.catalog.infrastructure.ProductRepository;
import com.deep.ordexaflow.common.exception.InvalidRequestException;
import com.deep.ordexaflow.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class ProductServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-20T20:00:00Z");

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private ProductService productService;
    private Category category;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        productService = new ProductService(
                productRepository, categoryRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        category = new Category("Laptops", "laptops", NOW);
    }

    @Test
    void createsProductWithNormalizedSkuCurrencyAndInitialInventory() {
        when(categoryRepository.findByIdAndActiveTrue(category.getId())).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = productService.create(new CreateProductRequest(
                " laptop-001 ", "Developer Laptop", "Portable workstation",
                new BigDecimal("1299.00"), "usd", category.getId(), 8));

        assertThat(response.sku()).isEqualTo("LAPTOP-001");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.availableQuantity()).isEqualTo(8);
        assertThat(response.category().slug()).isEqualTo("laptops");
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsBoundedFilteredProductPage() {
        Product product = product(8);
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "price"));
        when(productRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        var response = productService.search(
                "laptops", new BigDecimal("100"), new BigDecimal("2000"), "developer", pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().name()).isEqualTo("Developer Laptop");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void rejectsUnsupportedSortAndInvalidPriceRange() {
        assertThatThrownBy(() -> productService.search(
                null, null, null, null, PageRequest.of(0, 20, Sort.by("unknown"))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Unsupported sort field: unknown");

        assertThatThrownBy(() -> productService.search(
                null, new BigDecimal("100"), new BigDecimal("10"), null, PageRequest.of(0, 20)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Minimum price must not exceed maximum price");
    }

    @Test
    void hidesDiscontinuedProductFromPublicDetail() {
        Product product = product(1);
        product.discontinue(NOW);
        when(productRepository.findOneById(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.getPublicProduct(product.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatesInventoryWithoutReplacingProduct() {
        Product product = product(1);
        when(productRepository.findOneById(product.getId())).thenReturn(Optional.of(product));

        var response = productService.setInventory(product.getId(), new InventoryUpdateRequest(25));

        assertThat(response.availableQuantity()).isEqualTo(25);
        assertThat(product.getInventory().getUpdatedAt()).isEqualTo(NOW);
    }

    private Product product(int quantity) {
        return new Product(
                category, "LAPTOP-001", "Developer Laptop", "Portable workstation",
                new BigDecimal("1299.00"), "USD", quantity, NOW.minusSeconds(60));
    }
}
