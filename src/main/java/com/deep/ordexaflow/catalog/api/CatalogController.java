package com.deep.ordexaflow.catalog.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.deep.ordexaflow.catalog.application.CategoryService;
import com.deep.ordexaflow.catalog.application.ProductService;
import com.deep.ordexaflow.common.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CategoryService categoryService;
    private final ProductService productService;

    public CatalogController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categoryService.listActive();
    }

    @GetMapping("/products")
    public PageResponse<ProductResponse> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(name = "q", required = false) String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return productService.search(category, minPrice, maxPrice, query, pageable);
    }

    @GetMapping("/products/{productId}")
    public ProductResponse product(@PathVariable UUID productId) {
        return productService.getPublicProduct(productId);
    }
}
