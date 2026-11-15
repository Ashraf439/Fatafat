package com.ashraf.rider.dto;

import jakarta.validation.constraints.NotNull;

public record AvailabilityRequest(@NotNull Boolean online, Double latitude, Double longitude) {}