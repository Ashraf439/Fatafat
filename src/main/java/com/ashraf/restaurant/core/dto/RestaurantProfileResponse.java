package com.ashraf.restaurant.core.dto;

import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class RestaurantProfileResponse {
    private final UUID id;
    private final String name;
    private final String ownerName;
    private final RestaurantOnboardingStatus onboardingStatus;
    private final Boolean isOpen;
    private final String imageUrl;
    private final String street;
    private final String landmark;
    private final String city;
    private final String state;
    private final String pincode;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String fssaiLicense;
    private final String gstin;

    public RestaurantProfileResponse(UUID id, String name, String ownerName, RestaurantOnboardingStatus onboardingStatus,
                                     Boolean isOpen, String imageUrl, String street, String landmark, String city,
                                     String state, String pincode, BigDecimal latitude, BigDecimal longitude,
                                     String fssaiLicense, String gstin) {
        this.id = id;
        this.name = name;
        this.ownerName = ownerName;
        this.onboardingStatus = onboardingStatus;
        this.isOpen = isOpen;
        this.imageUrl = imageUrl;
        this.street = street;
        this.landmark = landmark;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.fssaiLicense = fssaiLicense;
        this.gstin = gstin;
    }
}