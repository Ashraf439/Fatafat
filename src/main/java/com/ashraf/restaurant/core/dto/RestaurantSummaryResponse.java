package com.ashraf.restaurant.core.dto;

import lombok.Getter;

@Getter
public class RestaurantSummaryResponse {
    private final Long id;
    private final String name;
    private final Boolean isOpen;
    private final String street;
    private final String city;
    private final String imageUrl;

    public RestaurantSummaryResponse(Long id, String name, Boolean isOpen,
                                     String street, String city, String imageUrl) {
        this.id = id;
        this.name = name;
        this.isOpen = isOpen;
        this.street = street;
        this.city = city;
        this.imageUrl = imageUrl;
    }
}