package com.ashraf.restaurant.core.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class TimingSlotRequest {
    @NotNull
    private DayOfWeek dayOfWeek;

    @NotNull
    private LocalTime openTime;

    @NotNull
    private LocalTime closeTime;
}