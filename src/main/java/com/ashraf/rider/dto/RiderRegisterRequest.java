package com.ashraf.rider.dto;

import com.ashraf.commerce.dto.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiderRegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be a valid E.164 format (e.g., +1234567890)")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
            message = "Password must contain at least one digit, one lowercase, one uppercase letter, and one special character"
    )
    private String password;

    // --- Enhanced Vehicle Details ---

    @NotBlank(message = "Vehicle type is required")
    @Pattern(regexp = "^(BIKE|CAR|SCOOTER|VAN)$", message = "Vehicle type must be BIKE, CAR, SCOOTER, or VAN")
    private String vehicleType;

    @NotBlank(message = "Vehicle registration number is required")
    @Pattern(regexp = "^[A-Z0-9-]{5,15}$", message = "Invalid vehicle registration or license plate number")
    private String vehicleNumber; // e.g., "NY-123XYZ" or state equivalents

    @NotBlank(message = "Vehicle model is required")
    @Size(max = 50, message = "Vehicle model details cannot exceed 50 characters")
    private String vehicleModel; // e.g., "Honda Civic 2022"

    @NotBlank(message = "Vehicle color is required")
    private String vehicleColor;

    // --- Enhanced Legal & Compliance Details ---

    @NotBlank(message = "Driving license is required")
    @Size(min = 15, max = 17, message = "Driving license must be between 15 and 17 characters")
    @Pattern(regexp = "^[A-Z0-9/_-]+$", message = "Driving license number contains invalid characters")
    private String drivingLicenseNumber;

    @NotBlank(message = "National identity number is required")
    @Size(min = 5, max = 20, message = "National ID must be between 5 and 20 characters")
    private String nationalId; // SSN, Aadhaar, or Passport based on country logic

    // --- Optional Onboarding Preferences ---

    @NotNull
    @Valid
    private AddressRequest address;

    private String referralCode; // For marketing trackbacks
}
