package com.deep.ordexaflow.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.deep.ordexaflow.common.exception.ResourceNotFoundException;
import com.deep.ordexaflow.users.domain.Role;
import com.deep.ordexaflow.users.domain.User;
import com.deep.ordexaflow.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CurrentUserServiceTest {
    private UserRepository userRepository;
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        currentUserService = new CurrentUserService(userRepository);
    }

    @Test
    void returnsCurrentUserProfile() {
        User user = new User("Deep", "Patel", "deep@example.com", "password-hash", new Role("ROLE_USER"));
        when(userRepository.findOneById(user.getId())).thenReturn(Optional.of(user));

        var response = currentUserService.getCurrentUser(user.getId());

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.firstName()).isEqualTo("Deep");
        assertThat(response.lastName()).isEqualTo("Patel");
        assertThat(response.email()).isEqualTo("deep@example.com");
        assertThat(response.roles()).isEqualTo(Set.of("ROLE_USER"));
        assertThat(response.createdAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    void rejectsTokenForUserThatNoLongerExists() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findOneById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentUserService.getCurrentUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: " + userId);
    }
}
