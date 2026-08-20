package com.deep.ordexaflow.auth.application;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deep.ordexaflow.auth.api.RegisterRequest;
import com.deep.ordexaflow.auth.api.UserRegistrationResponse;
import com.deep.ordexaflow.common.exception.DuplicateEmailException;
import com.deep.ordexaflow.users.domain.Role;
import com.deep.ordexaflow.users.domain.User;
import com.deep.ordexaflow.users.infrastructure.RoleRepository;
import com.deep.ordexaflow.users.infrastructure.UserRepository;

@Service
public class RegistrationService {
    private static final String DEFAULT_ROLE = "ROLE_USER";
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserRegistrationResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        Role role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default user role is not configured"));
        User saved = userRepository.save(new User(
                request.firstName().trim(), request.lastName().trim(), email,
                passwordEncoder.encode(request.password()), role));
        Set<String> roles = saved.getRoles().stream().map(Role::getName)
                .collect(Collectors.toUnmodifiableSet());
        return new UserRegistrationResponse(saved.getId(), saved.getFirstName(), saved.getLastName(),
                saved.getEmail(), roles, saved.getCreatedAt());
    }
}
