package com.rallycourt.auth.repository;

import com.rallycourt.auth.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);
}
