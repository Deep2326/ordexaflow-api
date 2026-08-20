package com.deep.ordexaflow.users.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.deep.ordexaflow.users.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findOneById(UUID id);
}
