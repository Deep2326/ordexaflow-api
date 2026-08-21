package com.deep.ordexaflow.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import com.deep.ordexaflow.cart.infrastructure.CartRepository;
import com.deep.ordexaflow.catalog.infrastructure.ProductRepository;
import com.deep.ordexaflow.users.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(CartAuthorizationTest.MethodSecurityConfiguration.class)
class CartAuthorizationTest {
    private static final UUID USER_ID = UUID.fromString("b03c5d0e-a143-4e12-8812-f5647c1c93e3");

    @Autowired
    private CartService cartService;

    @MockitoBean
    private CartRepository cartRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private Clock clock;

    @Test
    @WithMockUser(roles = "USER")
    void permitsCustomerAtCartServiceBoundary() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        var response = cartService.getCart(USER_ID);

        assertThat(response.items()).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deniesAdminWithoutCustomerRoleAtCartServiceBoundary() {
        assertThatThrownBy(() -> cartService.getCart(USER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @Import(CartService.class)
    static class MethodSecurityConfiguration {
    }
}
