package com.deep.ordexaflow.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.deep.ordexaflow.auth.api.RegisterRequest;
import com.deep.ordexaflow.common.exception.DuplicateEmailException;
import com.deep.ordexaflow.users.domain.Role;
import com.deep.ordexaflow.users.domain.User;
import com.deep.ordexaflow.users.infrastructure.RoleRepository;
import com.deep.ordexaflow.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void normalizesInputHashesPasswordAndAssignsUserRole() {
        RegisterRequest request = new RegisterRequest(
                " Deep ", " Patel ", " Deep@Example.COM ", "StrongPassword123!");
        when(userRepository.existsByEmail("deep@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(new Role("ROLE_USER")));
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        var response = registrationService.register(request);

        assertThat(response.email()).isEqualTo("deep@example.com");
        assertThat(response.firstName()).isEqualTo("Deep");
        assertThat(response.lastName()).isEqualTo("Patel");
        assertThat(response.roles()).containsExactly("ROLE_USER");
        assertThat(response.id()).isNotNull();
        verify(passwordEncoder).encode("StrongPassword123!");
    }

    @Test
    void rejectsDuplicateNormalizedEmail() {
        RegisterRequest request = new RegisterRequest(
                "Deep", "Patel", " Deep@Example.COM ", "StrongPassword123!");
        when(userRepository.existsByEmail("deep@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("deep@example.com");
    }
}
