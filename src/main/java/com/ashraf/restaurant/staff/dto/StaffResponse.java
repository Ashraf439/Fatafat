package com.ashraf.restaurant.staff.dto;

import com.ashraf.restaurant.staff.enums.StaffStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class StaffResponse {
    private UUID userId;
    private String email;
    private String role;
    private String status;

    public StaffResponse(UUID userId, String email, String role, StaffStatus status) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.status = status != null ? status.name() : null;
    }
}