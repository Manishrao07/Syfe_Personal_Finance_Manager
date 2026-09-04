package com.syfe.financemanager.repository;

import com.syfe.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for {@link User} accounts. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
