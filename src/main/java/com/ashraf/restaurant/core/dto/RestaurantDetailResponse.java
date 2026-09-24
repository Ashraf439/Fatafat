package com.ashraf.restaurant.core.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** Header data for a restaurant page: identity, address, opening hours and menu stats. */
@Getter
public class RestaurantDetailResponse {
    private final Long id;
    private final String name;
    private final Boolean isOpen;
    private final String imageUrl;
    private final String street;
    private final String landmark;
    private final String city;
    private final String state;
    private final String pincode;
    private final List<TimingSlotResponse> timings;
    private final Integer menuItemCount;
    private final BigDecimal startingPrice;
    private final Integer avgPrepTimeMinutes;
    private final Boolean pureVeg;

    public RestaurantDetailResponse(Long id, String name, Boolean isOpen, String imageUrl,
                                    String street, String landmark, String city, String state,
                                    String pincode, List<TimingSlotResponse> timings,
                                    Integer menuItemCount, BigDecimal startingPrice,
                                    Integer avgPrepTimeMinutes, Boolean pureVeg) {
        this.id = id;
        this.name = name;
        this.isOpen = isOpen;
        this.imageUrl = imageUrl;
        this.street = street;
        this.landmark = landmark;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.timings = timings;
        this.menuItemCount = menuItemCount;
        this.startingPrice = startingPrice;
        this.avgPrepTimeMinutes = avgPrepTimeMinutes;
        this.pureVeg = pureVeg;
    }
}
