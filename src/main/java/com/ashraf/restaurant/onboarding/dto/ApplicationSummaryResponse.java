package com.ashraf.restaurant.onboarding.dto;

import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;

public record ApplicationSummaryResponse(
        Long id,
        String restaurantName,
        String ownerEmail,
        String city,
        String state,
        RestaurantOnboardingStatus status,
        Integer attemptNumber
) {}