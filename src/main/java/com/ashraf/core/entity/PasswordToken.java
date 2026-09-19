package com.ashraf.core.entity;

import com.ashraf.core.enums.PasswordTokenPurpose;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Single-use token for setting/resetting a password. Only the SHA-256 hash of the token is stored
 * (same approach as refresh tokens); the raw value only ever exists in the emailed link.
 */
@Entity
@Table(name = "password_tokens")
@Getter
@Setter
public class PasswordToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PasswordTokenPurpose purpose;

    @Column(nullable = false)
    private Instant expiresAt;
}
