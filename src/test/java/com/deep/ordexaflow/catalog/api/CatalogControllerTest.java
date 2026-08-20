package com.deep.ordexaflow.catalog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.deep.ordexaflow.catalog.application.CategoryService;
import com.deep.ordexaflow.catalog.application.ProductService;
import com.deep.ordexaflow.common.web.GlobalExceptionHandler;
import com.deep.ordexaflow.common.web.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CatalogControllerTest {
    private ProductService productService;
    private CategoryService categoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        categoryService = mock(CategoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CatalogController(categoryService, productService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listsActiveCategories() throws Exception {
        when(categoryService.listActive()).thenReturn(List.of(new CategoryResponse(
                UUID.randomUUID(), "Laptops", "laptops", true,
                Instant.parse("2026-08-20T20:00:00Z"), Instant.parse("2026-08-20T20:00:00Z"))));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("laptops"));
    }

    @Test
    void returnsPaginatedProducts() throws Exception {
        ProductResponse product = productResponse();
        when(productService.search(any(), any(), any(), any(), any())).thenReturn(
                new PageResponse<>(List.of(product), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/products")
                        .param("category", "laptops")
                        .param("minPrice", "100")
                        .param("maxPrice", "2000")
                        .param("q", "developer")
                        .param("sort", "price,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private ProductResponse productResponse() {
        Instant now = Instant.parse("2026-08-20T20:00:00Z");
        return new ProductResponse(
                UUID.randomUUID(), "LAPTOP-001", "Developer Laptop", "Portable workstation",
                new BigDecimal("1299.00"), "USD", 8, true,
                new ProductResponse.CategorySummary(UUID.randomUUID(), "Laptops", "laptops"), now, now);
    }
}
