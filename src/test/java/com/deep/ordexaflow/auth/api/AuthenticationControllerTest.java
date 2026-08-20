package com.deep.ordexaflow.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.deep.ordexaflow.auth.application.LoginService;
import com.deep.ordexaflow.auth.application.InvalidRefreshTokenException;
import com.deep.ordexaflow.auth.application.RefreshTokenService;
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
    private RefreshTokenService refreshTokenService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        registrationService = mock(RegistrationService.class);
        loginService = mock(LoginService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AuthenticationController(registrationService, loginService, refreshTokenService))
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
        when(loginService.login(any())).thenReturn(new LoginResponse(
                "signed.jwt.token", "opaque-refresh-token", "Bearer", 900, 2_592_000));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"deep@example.com","password":"StrongPassword123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("opaque-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshExpiresIn").value(2_592_000));
    }

    @Test
    void rotatesRefreshToken() throws Exception {
        when(refreshTokenService.rotate("current-refresh-token")).thenReturn(new LoginResponse(
                "new-access-token", "new-refresh-token", "Bearer", 900, 2_592_000));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"current-refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void returnsUnauthorizedForInvalidRefreshToken() throws Exception {
        when(refreshTokenService.rotate("invalid-refresh-token"))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"invalid-refresh-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message").value("Refresh token is invalid or expired"));
    }

    @Test
    void revokesRefreshTokenOnLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"refresh-token-to-revoke"}
                                """))
                .andExpect(status().isNoContent());

        verify(refreshTokenService).revoke("refresh-token-to-revoke");
    }

    @Test
    void validatesMissingRefreshToken() throws Exception {
                mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("refreshToken"));
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
        var refresh = new RefreshTokenRequest("request-refresh-secret");
        var response = new LoginResponse(
                "secret.jwt.token", "response-refresh-secret", "Bearer", 900, 2_592_000);

        assertThat(login.toString()).contains("[REDACTED]").doesNotContain("LoginSecret123!");
        assertThat(register.toString()).contains("[REDACTED]").doesNotContain("RegisterSecret123!");
        assertThat(refresh.toString()).contains("[REDACTED]").doesNotContain("request-refresh-secret");
        assertThat(response.toString()).contains("[REDACTED]")
                .doesNotContain("secret.jwt.token", "response-refresh-secret");
    }
}
