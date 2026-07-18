package com.ashraf.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiderRegisterRequest {
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String phoneNumber;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    private String vehicleType;

    @NotBlank @Size(min = 15, max = 17, message = "Driving license must be between 15 and 17")
    private String drivingLicenseNumber;
}
