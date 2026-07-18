package com.ashraf.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantRegisterRequest {
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String phoneNumber;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank @Size(min = 14, max = 14, message = "fssaiNumber must be at least 14 characters")
    private String fssaiLicense;
}
