package com.deep.ordexaflow.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.deep.ordexaflow.users.domain.Role;
import com.deep.ordexaflow.users.domain.User;
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

@SpringJUnitConfig(CurrentUserAuthorizationTest.MethodSecurityConfiguration.class)
class CurrentUserAuthorizationTest {
    @Autowired
    private CurrentUserService currentUserService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void permitsAdminAtServiceBoundary() {
        User user = new User("Admin", "User", "admin@example.com", "password-hash", new Role("ROLE_ADMIN"));
        when(userRepository.findOneById(user.getId())).thenReturn(Optional.of(user));

        assertThat(currentUserService.getCurrentUser(user.getId()).email()).isEqualTo("admin@example.com");
    }

    @Test
    @WithMockUser(roles = "OTHER")
    void deniesUnexpectedRoleAtServiceBoundary() {
        User user = new User("Other", "User", "other@example.com", "password-hash", new Role("ROLE_OTHER"));

        assertThatThrownBy(() -> currentUserService.getCurrentUser(user.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @Import(CurrentUserService.class)
    static class MethodSecurityConfiguration {
    }
}
