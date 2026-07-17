package com.ashraf.repository;

import com.ashraf.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByUserRoles_Role_Name(String roleName);
}
