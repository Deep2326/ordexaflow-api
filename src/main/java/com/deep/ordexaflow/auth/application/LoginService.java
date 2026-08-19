package com.deep.ordexaflow.auth.application;

import java.util.Locale;

import com.deep.ordexaflow.auth.api.LoginRequest;
import com.deep.ordexaflow.auth.api.LoginResponse;
import com.deep.ordexaflow.users.domain.User;
import com.deep.ordexaflow.users.infrastructure.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public LoginService(AuthenticationManager authenticationManager, UserRepository userRepository,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        JwtService.IssuedToken token = jwtService.issueAccessToken(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds());
    }
}
