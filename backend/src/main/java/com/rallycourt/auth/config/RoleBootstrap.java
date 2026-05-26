package com.rallycourt.auth.config;

import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RoleBootstrap {

    private final RoleRepository roleRepository;

    @Bean
    ApplicationRunner roleBootstrapRunner() {
        return args -> {
            ensureRole("ADMIN", "Administrator", "System administrator");
            ensureRole("PLAYER", "Player", "Standard player account");
            ensureRole("COURT_OWNER", "Court Owner", "Approved court owner");
        };
    }

    private void ensureRole(String code, String name, String description) {
        if (roleRepository.findByCode(code).isPresent()) {
            return;
        }
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        roleRepository.save(role);
    }
}
