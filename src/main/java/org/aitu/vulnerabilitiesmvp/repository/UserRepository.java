package org.aitu.vulnerabilitiesmvp.repository;

import java.util.Optional;
import org.aitu.vulnerabilitiesmvp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
