package com.deep.ordexaflow.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deep.ordexaflow.auth.api.LoginRequest;
import com.deep.ordexaflow.users.domain.Role;
import com.deep.ordexaflow.users.domain.User;
import com.deep.ordexaflow.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

class LoginServiceTest {
    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private JwtService jwtService;
    private LoginService loginService;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        userRepository = mock(UserRepository.class);
        jwtService = mock(JwtService.class);
        loginService = new LoginService(authenticationManager, userRepository, jwtService);
    }

    @Test
    void authenticatesNormalizedEmailAndReturnsAccessToken() {
        User user = new User("Deep", "Patel", "deep@example.com", "password-hash", new Role("ROLE_USER"));
        when(userRepository.findByEmail("deep@example.com")).thenReturn(Optional.of(user));
        when(jwtService.issueAccessToken(user)).thenReturn(new JwtService.IssuedToken("signed-token", 900));

        var response = loginService.login(new LoginRequest("  Deep@Example.COM ", "StrongPassword123!"));

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authentication =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authentication.capture());
        assertThat(authentication.getValue().getPrincipal()).isEqualTo("deep@example.com");
        assertThat(authentication.getValue().getCredentials()).isEqualTo("StrongPassword123!");
        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
    }

    @Test
    void propagatesAuthenticationFailureWithoutIssuingToken() {
        var authentication = new UsernamePasswordAuthenticationToken("deep@example.com", "wrong-password");
        when(authenticationManager.authenticate(authentication))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> loginService.login(new LoginRequest("deep@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
