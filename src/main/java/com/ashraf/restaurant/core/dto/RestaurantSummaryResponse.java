package com.ashraf.restaurant.core.dto;

import lombok.Getter;

import java.math.BigDecimal;

/** Card-level view of a restaurant for customer browsing. */
@Getter
public class RestaurantSummaryResponse {
    private final Long id;
    private final String name;
    private final Boolean isOpen;
    private final String street;
    private final String city;
    private final String imageUrl;
    private final Integer menuItemCount;
    private final BigDecimal startingPrice;
    private final Integer avgPrepTimeMinutes;
    private final Boolean pureVeg;

    public RestaurantSummaryResponse(Long id, String name, Boolean isOpen,
                                     String street, String city, String imageUrl,
                                     Integer menuItemCount, BigDecimal startingPrice,
                                     Integer avgPrepTimeMinutes, Boolean pureVeg) {
        this.id = id;
        this.name = name;
        this.isOpen = isOpen;
        this.street = street;
        this.city = city;
        this.imageUrl = imageUrl;
        this.menuItemCount = menuItemCount;
        this.startingPrice = startingPrice;
        this.avgPrepTimeMinutes = avgPrepTimeMinutes;
        this.pureVeg = pureVeg;
    }
}
