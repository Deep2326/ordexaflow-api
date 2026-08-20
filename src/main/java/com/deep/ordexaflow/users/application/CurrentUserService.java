package com.deep.ordexaflow.users.application;

import java.util.UUID;
import java.util.stream.Collectors;

import com.deep.ordexaflow.common.exception.ResourceNotFoundException;
import com.deep.ordexaflow.users.api.UserProfileResponse;
import com.deep.ordexaflow.users.domain.Role;
import com.deep.ordexaflow.users.domain.User;
import com.deep.ordexaflow.users.infrastructure.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(UUID userId) {
        User user = userRepository.findOneById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        var roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toUnmodifiableSet());
        return new UserProfileResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                roles, user.getCreatedAt());
    }
}
