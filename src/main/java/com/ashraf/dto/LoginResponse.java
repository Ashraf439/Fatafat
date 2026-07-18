package com.ashraf.dto;

import lombok.Getter;

@Getter
public class LoginResponse {
    private String token;
    private String status;
    private Long userId;

    public LoginResponse(String token, String status, Long userId) {
        this.token = token;
        this.status = status;
        this.userId = userId;
    }
}
