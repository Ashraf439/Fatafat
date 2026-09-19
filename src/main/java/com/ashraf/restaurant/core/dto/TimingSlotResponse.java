package com.ashraf.restaurant.core.dto;

import com.ashraf.restaurant.core.entity.RestaurantTimings;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
public class TimingSlotResponse {
    private final Long id;
    private final DayOfWeek dayOfWeek;
    private final LocalTime openTime;
    private final LocalTime closeTime;

    public TimingSlotResponse(RestaurantTimings timing) {
        this.id = timing.getId();
        this.dayOfWeek = timing.getDayOfWeek();
        this.openTime = timing.getOpenTime();
        this.closeTime = timing.getCloseTime();
    }

    // For slots that don't have their own id yet (e.g. an onboarding application's
    // embedded shifts, before they become real RestaurantTimings rows on approval).
    public TimingSlotResponse(DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime) {
        this.id = null;
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }
}