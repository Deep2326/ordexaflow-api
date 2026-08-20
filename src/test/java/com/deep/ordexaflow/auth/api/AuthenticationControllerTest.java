package com.deep.ordexaflow.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.deep.ordexaflow.auth.application.LoginService;
import com.deep.ordexaflow.auth.application.RegistrationService;
import com.deep.ordexaflow.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthenticationControllerTest {
    private RegistrationService registrationService;
    private LoginService loginService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        registrationService = mock(RegistrationService.class);
        loginService = mock(LoginService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthenticationController(registrationService, loginService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registersValidCustomer() throws Exception {
        when(registrationService.register(any())).thenReturn(new UserRegistrationResponse(
                UUID.randomUUID(), "Deep", "Patel", "deep@example.com",
                Set.of("ROLE_USER"), Instant.parse("2026-08-19T18:30:00Z")));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Deep","lastName":"Patel","email":"deep@example.com",
                                 "password":"StrongPassword123!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("deep@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void returnsStructuredValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Patel","email":"invalid","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void logsInWithValidCredentials() throws Exception {
        when(loginService.login(any())).thenReturn(new LoginResponse("signed.jwt.token", "Bearer", 900));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"deep@example.com","password":"StrongPassword123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void returnsUnauthorizedForInvalidCredentials() throws Exception {
        when(loginService.login(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"deep@example.com","password":"WrongPassword123!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void redactsCredentialsAndTokensFromStringRepresentations() {
        var login = new LoginRequest("deep@example.com", "LoginSecret123!");
        var register = new RegisterRequest("Deep", "Patel", "deep@example.com", "RegisterSecret123!");
        var response = new LoginResponse("secret.jwt.token", "Bearer", 900);

        assertThat(login.toString()).contains("[REDACTED]").doesNotContain("LoginSecret123!");
        assertThat(register.toString()).contains("[REDACTED]").doesNotContain("RegisterSecret123!");
        assertThat(response.toString()).contains("[REDACTED]").doesNotContain("secret.jwt.token");
    }
}
