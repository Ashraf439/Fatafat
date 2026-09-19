package com.ashraf.core.repository;

import com.ashraf.core.entity.PasswordToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordTokenRepository extends JpaRepository<PasswordToken, Long> {
    Optional<PasswordToken> findByTokenHash(String tokenHash);

    /** Call inside a transaction. Removes every outstanding token for the user. */
    void deleteByUser_Id(Long userId);
}
