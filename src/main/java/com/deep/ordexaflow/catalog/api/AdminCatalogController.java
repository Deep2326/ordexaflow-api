package com.deep.ordexaflow.catalog.api;

import java.util.UUID;

import com.deep.ordexaflow.catalog.application.CategoryService;
import com.deep.ordexaflow.catalog.application.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {
    private final CategoryService categoryService;
    private final ProductService productService;

    public AdminCatalogController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/categories/{categoryId}")
    public CategoryResponse updateCategory(
            @PathVariable UUID categoryId, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(categoryId, request);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/products/{productId}")
    public ProductResponse updateProduct(
            @PathVariable UUID productId, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(productId, request);
    }

    @DeleteMapping("/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discontinueProduct(@PathVariable UUID productId) {
        productService.discontinue(productId);
    }

    @PutMapping("/products/{productId}/inventory")
    public ProductResponse updateInventory(
            @PathVariable UUID productId, @Valid @RequestBody InventoryUpdateRequest request) {
        return productService.setInventory(productId, request);
    }
}
