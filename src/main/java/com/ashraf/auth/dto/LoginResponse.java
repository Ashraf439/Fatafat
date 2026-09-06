package com.ashraf.auth.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class LoginResponse {
    private Account account;
    private long expiresAt;

    public LoginResponse(Account account, long expiresAt) {
        this.account = account;
        this.expiresAt = expiresAt;
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
}