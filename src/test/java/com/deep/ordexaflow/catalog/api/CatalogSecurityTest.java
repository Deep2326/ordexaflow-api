package com.deep.ordexaflow.catalog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.deep.ordexaflow.catalog.application.CategoryService;
import com.deep.ordexaflow.catalog.application.ProductService;
import com.deep.ordexaflow.common.config.SecurityConfig;
import com.deep.ordexaflow.common.web.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({CatalogController.class, AdminCatalogController.class})
@Import(SecurityConfig.class)
class CatalogSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    void permitsPublicCatalogReadsWithoutToken() throws Exception {
        when(categoryService.listActive()).thenReturn(List.of());
        when(productService.search(any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void deniesCustomerCatalogMutation() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Laptops","slug":"laptops","active":true}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void permitsAdminCatalogMutation() throws Exception {
        UUID categoryId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T20:00:00Z");
        when(categoryService.create(any())).thenReturn(
                new CategoryResponse(categoryId, "Laptops", "laptops", true, now, now));

        mockMvc.perform(post("/api/v1/admin/categories")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Laptops","slug":"laptops","active":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("laptops"));
    }
}
