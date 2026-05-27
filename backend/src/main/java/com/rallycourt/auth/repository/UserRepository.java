package com.rallycourt.auth.repository;

import com.rallycourt.auth.entity.User;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    java.util.List<User> findByEmailIn(Collection<String> emails);
}
