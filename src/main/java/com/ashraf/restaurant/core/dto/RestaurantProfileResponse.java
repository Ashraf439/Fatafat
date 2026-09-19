package com.ashraf.restaurant.core.dto;

import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import lombok.Getter;

/**
 * "My restaurant" profile for the owner's dashboard. Before go-live (application
 * under review or approved-pending-payment) id/isOpen/imageUrl/address details/
 * license numbers are null since there's no Restaurant row yet - only name,
 * ownerName, onboardingStatus and city/state (sourced from the application) are
 * available at that stage.
 */
@Getter
public class RestaurantProfileResponse {
    private final Long id;
    private final String name;
    private final String ownerName;
    private final RestaurantOnboardingStatus onboardingStatus;
    private final Boolean isOpen;
    private final String imageUrl;
    private final String street;
    private final String city;
    private final String state;
    private final String pincode;
    private final String fssaiLicense;
    private final String gstin;

    public RestaurantProfileResponse(Long id, String name, String ownerName, RestaurantOnboardingStatus onboardingStatus,
                                     Boolean isOpen, String imageUrl, String street, String city, String state,
                                     String pincode, String fssaiLicense, String gstin) {
        this.id = id;
        this.name = name;
        this.ownerName = ownerName;
        this.onboardingStatus = onboardingStatus;
        this.isOpen = isOpen;
        this.imageUrl = imageUrl;
        this.street = street;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.fssaiLicense = fssaiLicense;
        this.gstin = gstin;
    }
}