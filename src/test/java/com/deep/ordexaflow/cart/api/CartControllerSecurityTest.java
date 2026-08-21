package com.deep.ordexaflow.cart.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.deep.ordexaflow.cart.application.InsufficientInventoryException;
import com.deep.ordexaflow.cart.application.CartService;
import com.deep.ordexaflow.common.config.SecurityConfig;
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

@WebMvcTest(CartController.class)
@Import(SecurityConfig.class)
class CartControllerSecurityTest {
    private static final UUID USER_ID = UUID.fromString("b03c5d0e-a143-4e12-8812-f5647c1c93e3");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    void rejectsCartRequestWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    void returnsAuthenticatedUsersCart() throws Exception {
        when(cartService.getCart(USER_ID)).thenReturn(CartResponse.empty("USD"));

        mockMvc.perform(get("/api/v1/cart")
                        .with(customerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.currency").value("USD"));

        verify(cartService).getCart(USER_ID);
    }

    @Test
    void rejectsInvalidAddItemRequest() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void addsItemUsingUserIdFromJwt() throws Exception {
        UUID productId = UUID.randomUUID();
        when(cartService.addItem(eq(USER_ID), any())).thenReturn(CartResponse.empty("USD"));

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":2}
                                """.formatted(productId)))
                .andExpect(status().isOk());

        verify(cartService).addItem(eq(USER_ID), any(AddCartItemRequest.class));
    }

    @Test
    void returnsStructuredInventoryConflict() throws Exception {
        UUID productId = UUID.randomUUID();
        when(cartService.addItem(eq(USER_ID), any()))
                .thenThrow(new InsufficientInventoryException(productId, 3, 2));

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":3}
                                """.formatted(productId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_INVENTORY"))
                .andExpect(jsonPath("$.path").value("/api/v1/cart/items"));
    }

    @Test
    void clearsAuthenticatedUsersCart() throws Exception {
        mockMvc.perform(delete("/api/v1/cart")
                        .with(customerJwt()))
                .andExpect(status().isNoContent());

        verify(cartService).clear(USER_ID);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor customerJwt() {
        return jwt()
                .jwt(token -> token.claim("userId", USER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
