package com.ashraf.restaurant.core.dto;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class CustomerMenuResponse {
    private final Long restaurantId;
    private final String restaurantName;
    private final Boolean isOpen;
    private final Map<String, List<MenuResponse>> categories;

    public CustomerMenuResponse(Long restaurantId, String restaurantName, Boolean isOpen, Map<String, List<MenuResponse>> categories) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.isOpen = isOpen;
        this.categories = categories;
    }
}
