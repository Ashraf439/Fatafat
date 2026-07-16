package com.ashraf.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank
    private String name;

    @Email @NotBlank
    private String email;

    @NotBlank
    private String phoneNumber;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull
    private Role role;

    private String fssaiLicense;
    private String vehicleType;
    private String licenseNumber;

    private String city;
    private String state;
    private String pincode;
    private String street;
    private Double latitude;
    private Double longitude;

}