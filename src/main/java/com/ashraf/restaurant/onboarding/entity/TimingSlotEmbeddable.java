package com.ashraf.restaurant.onboarding.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Embeddable
@Getter
@Setter
public class TimingSlotEmbeddable {

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private LocalTime openTime;

    private LocalTime closeTime;
}