package com.ashraf.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRegisterRequest {
    @NotBlank
    private String name;

    @Email @NotBlank
    private String email;

    @NotBlank
    private String phoneNumber;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

}