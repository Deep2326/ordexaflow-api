package com.deep.ordexaflow.users.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.deep.ordexaflow.common.config.SecurityConfig;
import com.deep.ordexaflow.common.exception.ResourceNotFoundException;
import com.deep.ordexaflow.users.application.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CurrentUserController.class)
@Import(SecurityConfig.class)
class CurrentUserControllerSecurityTest {
    private static final UUID USER_ID = UUID.fromString("b03c5d0e-a143-4e12-8812-f5647c1c93e3");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    void rejectsRequestWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me"));
    }

    @Test
    void rejectsInvalidAccessTokenWithStructuredError() throws Exception {
        when(jwtDecoder.decode("invalid-token")).thenThrow(new BadJwtException("Invalid token"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required or the access token is invalid"));
    }

    @Test
    void rejectsExpiredAccessTokenWithStructuredError() throws Exception {
        var expirationError = new OAuth2Error("invalid_token", "The access token expired", null);
        when(jwtDecoder.decode("expired-token")).thenThrow(new JwtValidationException(
                "The access token expired", List.of(expirationError)));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required or the access token is invalid"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me"));
    }

    @Test
    void returnsProfileForAuthenticatedCustomer() throws Exception {
        when(currentUserService.getCurrentUser(USER_ID)).thenReturn(new UserProfileResponse(
                USER_ID, "Deep", "Patel", "deep@example.com", Set.of("ROLE_USER"),
                Instant.parse("2026-08-19T18:30:00Z")));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(jwt()
                                .jwt(token -> token.claim("userId", USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("deep@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        verify(currentUserService).getCurrentUser(USER_ID);
    }

    @Test
    void deniesCustomerAccessToAdminUrls() throws Exception {
        mockMvc.perform(get("/api/v1/admin/security-check")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void permitsAdminThroughUrlAuthorization() throws Exception {
        mockMvc.perform(get("/api/v1/admin/security-check")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void permitsPublicCatalogReadsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/products/not-yet-implemented"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundWhenTokenUserNoLongerExists() throws Exception {
        when(currentUserService.getCurrentUser(USER_ID)).thenThrow(new ResourceNotFoundException("User", USER_ID));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(jwt()
                                .jwt(token -> token.claim("userId", USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User not found: " + USER_ID));
    }
}
