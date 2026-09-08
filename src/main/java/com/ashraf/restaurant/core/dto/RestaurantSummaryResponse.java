package com.ashraf.restaurant.core.dto;

import lombok.Getter;

@Getter
public class RestaurantSummaryResponse {
    private Long id;
    private String name;
    private boolean isOpen;
    private String city;

    public RestaurantSummaryResponse(Long id, String name, boolean isOpen, String city) {
        this.id = id;
        this.name = name;
        this.isOpen = isOpen;
        this.city = city;
    }
}
