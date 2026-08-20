package com.deep.ordexaflow.catalog.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.deep.ordexaflow.catalog.api.CreateProductRequest;
import com.deep.ordexaflow.catalog.api.InventoryUpdateRequest;
import com.deep.ordexaflow.catalog.api.ProductResponse;
import com.deep.ordexaflow.catalog.api.UpdateProductRequest;
import com.deep.ordexaflow.catalog.domain.Category;
import com.deep.ordexaflow.catalog.domain.Product;
import com.deep.ordexaflow.catalog.infrastructure.CategoryRepository;
import com.deep.ordexaflow.catalog.infrastructure.ProductRepository;
import com.deep.ordexaflow.catalog.infrastructure.ProductSpecifications;
import com.deep.ordexaflow.common.exception.DuplicateResourceException;
import com.deep.ordexaflow.common.exception.InvalidRequestException;
import com.deep.ordexaflow.common.exception.ResourceNotFoundException;
import com.deep.ordexaflow.common.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS = Set.of("sku", "name", "price", "createdAt");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, Clock clock) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(
            String category, BigDecimal minPrice, BigDecimal maxPrice, String query, Pageable pageable) {
        validateSearch(category, minPrice, maxPrice, query, pageable);
        var page = productRepository.findAll(
                ProductSpecifications.publicCatalog(category, minPrice, maxPrice, query), pageable)
                .map(ProductResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublicProduct(UUID productId) {
        Product product = findProduct(productId);
        if (!product.isActive() || !product.getCategory().isActive()) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return ProductResponse.from(product);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse create(CreateProductRequest request) {
        String sku = normalizeSku(request.sku());
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateResourceException("Product", "sku", sku);
        }
        Category category = activeCategory(request.categoryId());
        Instant now = clock.instant();
        Product product = new Product(
                category, sku, request.name().trim(), request.description().trim(), request.price(),
                normalizeCurrency(request.currency()), request.initialQuantity(), now);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(UUID productId, UpdateProductRequest request) {
        Product product = findProduct(productId);
        String sku = normalizeSku(request.sku());
        if (productRepository.existsBySkuAndIdNot(sku, productId)) {
            throw new DuplicateResourceException("Product", "sku", sku);
        }
        Category category = activeCategory(request.categoryId());
        product.update(
                category, sku, request.name().trim(), request.description().trim(), request.price(),
                normalizeCurrency(request.currency()), request.active(), clock.instant());
        return ProductResponse.from(product);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void discontinue(UUID productId) {
        findProduct(productId).discontinue(clock.instant());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse setInventory(UUID productId, InventoryUpdateRequest request) {
        Product product = findProduct(productId);
        product.getInventory().setQuantity(request.quantity(), clock.instant());
        return ProductResponse.from(product);
    }

    private Product findProduct(UUID productId) {
        return productRepository.findOneById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    private Category activeCategory(UUID categoryId) {
        return categoryRepository.findByIdAndActiveTrue(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Active category", categoryId));
    }

    private void validateSearch(
            String category, BigDecimal minPrice, BigDecimal maxPrice, String query, Pageable pageable) {
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (category != null && category.length() > 120) {
            throw new InvalidRequestException("Category filter must not exceed 120 characters");
        }
        if (query != null && query.length() > 200) {
            throw new InvalidRequestException("Search query must not exceed 200 characters");
        }
        if ((minPrice != null && minPrice.signum() < 0)
                || (maxPrice != null && maxPrice.signum() < 0)) {
            throw new InvalidRequestException("Price filters must not be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidRequestException("Minimum price must not exceed maximum price");
        }
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidRequestException("Unsupported sort field: " + order.getProperty());
            }
        });
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrency(String currency) {
        return currency.trim().toUpperCase(Locale.ROOT);
    }
}
