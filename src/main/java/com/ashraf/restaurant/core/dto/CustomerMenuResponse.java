package com.ashraf.restaurant.core.dto;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class CustomerMenuResponse {
    private final UUID restaurantId;
    private final String restaurantName;
    private final Boolean isOpen;
    private final String imageUrl;
    private final Map<String, List<MenuResponse>> categories;

    public CustomerMenuResponse(UUID restaurantId, String restaurantName, Boolean isOpen, String imageUrl, Map<String, List<MenuResponse>> categories) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.isOpen = isOpen;
        this.imageUrl = imageUrl;
        this.categories = categories;
    }
}