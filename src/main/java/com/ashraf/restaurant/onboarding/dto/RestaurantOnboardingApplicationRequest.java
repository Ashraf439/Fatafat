package com.ashraf.restaurant.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantOnboardingApplicationRequest {

    @NotBlank
    private String restaurantName;

    @NotBlank
    private String addressLine;

    @NotBlank
    private String city;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    @NotBlank
    @Pattern(regexp = "^[0-9]{14}$", message = "FSSAI code must be 14 digits")
    private String fssaiLicense;

    @NotBlank
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
    private String gstin;

    @NotBlank
    private String accountHolderName;

    @NotBlank
    @Pattern(regexp = "^[0-9]{9,18}$", message = "Account number must be 9-18 digits")
    private String accountNumber;

    @NotBlank
    private String bankName;


    @NotBlank
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code")
    private String ifscCode;

    // getters/setters — write these yourself
}