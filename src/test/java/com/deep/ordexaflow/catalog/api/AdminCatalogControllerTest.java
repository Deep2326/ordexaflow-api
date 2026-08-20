package com.deep.ordexaflow.catalog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.deep.ordexaflow.catalog.application.CategoryService;
import com.deep.ordexaflow.catalog.application.ProductService;
import com.deep.ordexaflow.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminCatalogControllerTest {
    private CategoryService categoryService;
    private ProductService productService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        categoryService = mock(CategoryService.class);
        productService = mock(ProductService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCatalogController(categoryService, productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createsCategory() throws Exception {
        UUID categoryId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T20:00:00Z");
        when(categoryService.create(any())).thenReturn(
                new CategoryResponse(categoryId, "Laptops", "laptops", true, now, now));

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Laptops","slug":"laptops","active":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(categoryId.toString()));
    }

    @Test
    void returnsStructuredValidationForInvalidProduct() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"","name":"","description":"","price":0,
                                 "currency":"US","initialQuantity":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void discontinuesProduct() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/products/{productId}", productId))
                .andExpect(status().isNoContent());

        verify(productService).discontinue(productId);
    }
}
