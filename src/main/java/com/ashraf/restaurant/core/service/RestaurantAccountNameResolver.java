package com.ashraf.restaurant.core.service;

import com.ashraf.auth.spi.AccountNameResolver;
import com.ashraf.core.entity.User;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.onboarding.dto.ApplicationSummaryResponse;
import com.ashraf.restaurant.onboarding.service.RestaurantOnboardingService;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Owner's display name comes from Restaurant.ownerName once the restaurant is live,
 * or from the onboarding application before that. Staff resolve to empty since
 * there is no name field for staff yet.
 */
@Component
public class RestaurantAccountNameResolver implements AccountNameResolver {

    private final RestaurantAccessService restaurantAccessService;
    private final RestaurantOnboardingService restaurantOnboardingService;

    public RestaurantAccountNameResolver(RestaurantAccessService restaurantAccessService,
                                         RestaurantOnboardingService restaurantOnboardingService) {
        this.restaurantAccessService = restaurantAccessService;
        this.restaurantOnboardingService = restaurantOnboardingService;
    }

    @Override
    public Optional<String> resolveName(User user) {
        Optional<Restaurant> restaurant = restaurantAccessService.findRestaurantForUser(user);

        if (restaurant.isPresent()) {
            Restaurant r = restaurant.get();
            if (!restaurantAccessService.isOwner(user, r.getId())) {
                return Optional.empty();
            }
            if (r.getOwnerName() != null && !r.getOwnerName().isBlank()) {
                return Optional.of(r.getOwnerName());
            }
        }

        return restaurantOnboardingService.getMyApplication(user)
                .map(ApplicationSummaryResponse::ownerName)
                .filter(name -> !name.isBlank());
    }
}