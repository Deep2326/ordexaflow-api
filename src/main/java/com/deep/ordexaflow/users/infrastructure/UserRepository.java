package com.deep.ordexaflow.users.infrastructure;

import java.util.UUID;
import com.deep.ordexaflow.users.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
}
