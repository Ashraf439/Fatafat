package com.ashraf.restaurant.core.dto;

import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import lombok.Getter;

import java.util.UUID;

@Getter
public class OwnedRestaurantResponse {
    private final UUID id;
    private final String name;
    private final String city;
    private final boolean isOpen;
    private final RestaurantOnboardingStatus status;
    private final boolean ownedByMe;

    public OwnedRestaurantResponse(UUID id, String name, String city, boolean isOpen,
                                   RestaurantOnboardingStatus status, boolean ownedByMe) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.isOpen = isOpen;
        this.status = status;
        this.ownedByMe = ownedByMe;
    }
}