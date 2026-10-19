package com.ashraf.restaurant.onboarding.dto;

import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;

import java.util.UUID;

public record ApplicationSummaryResponse(
        UUID id,
        String restaurantName,
        String ownerEmail,
        String city,
        String state,
        RestaurantOnboardingStatus status,
        Integer attemptNumber,
        String ownerName
) {}