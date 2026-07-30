package com.ashraf.auth.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class LoginResponse {
    private Account account;
    private Tokens tokens;

    public LoginResponse(Account account, Tokens tokens) {
        this.account = account;
        this.tokens = tokens;
    }

    @Getter
    public static class Account {
        private Long id;
        private String email;
        private String phoneNumber;
        private String status;
        private List<String> roles;
        private String createdAt;

        public Account(Long id, String email, String phoneNumber, String status, List<String> roles, String createdAt) {
            this.id = id;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.status = status;
            this.roles = roles;
            this.createdAt = createdAt;
        }
    }

    @Getter
    public static class Tokens {
        private String accessToken;
        private String refreshToken;
        private long expiresAt;

        public Tokens(String accessToken, String refreshToken, long expiresAt) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAt = expiresAt;
        }
    }
}