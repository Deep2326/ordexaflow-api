package com.deep.ordexaflow.users.infrastructure;

import java.util.Optional;
import com.deep.ordexaflow.users.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByName(String name);
}
