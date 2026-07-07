package com.ashraf.dto;

import lombok.Getter;

@Getter
public class LoginResponse {
    private String token;
    private String role;
    private String status;
    private Long userId;

    public LoginResponse(String token, String role, String status, Long userId) {
        this.token = token;
        this.role = role;
        this.status = status;
        this.userId = userId;
    }
}
