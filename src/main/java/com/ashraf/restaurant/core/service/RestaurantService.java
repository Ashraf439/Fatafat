package com.ashraf.restaurant.core.service;

import com.ashraf.core.entity.User;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantAccessService restaurantAccessService;

    public RestaurantService(RestaurantRepository restaurantRepository,
                             RestaurantAccessService restaurantAccessService) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantAccessService = restaurantAccessService;
    }

    public boolean getStatus(User user) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(user);
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"))
                .getIsOpen();
    }

    @Transactional
    public boolean toggleStatus(User user) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(user);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        restaurant.setIsOpen(!restaurant.getIsOpen());
        restaurantRepository.save(restaurant);
        return restaurant.getIsOpen();
    }
}