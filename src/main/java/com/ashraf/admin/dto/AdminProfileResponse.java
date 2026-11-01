package com.ashraf.admin.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class AdminProfileResponse {
    private final Long id;
    private final String name;
    private final String email;
    private final String phoneNumber;
    private final List<String> roles;

    public AdminProfileResponse(Long id, String name, String email, String phoneNumber, List<String> roles) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.roles = roles;
    }
}