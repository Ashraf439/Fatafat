package com.ashraf.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class LoginResponse {
    private String accessToken;
    private UserSummary user;

    public LoginResponse(String accessToken, UserSummary user) {
        this.accessToken = accessToken;
        this.user = user;
    }

    @Getter
    public static class UserSummary {
        private Long id;
        private String email;
        private String phoneNumber;
        private String status;
        private List<String> roles;
        private String createdAt;

        public UserSummary(Long id, String email, String phoneNumber, String status, List<String> roles, String createdAt) {
            this.id = id;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.status = status;
            this.roles = roles;
            this.createdAt = createdAt;
        }
    }
}