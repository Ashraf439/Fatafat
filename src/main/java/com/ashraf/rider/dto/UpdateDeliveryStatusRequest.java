package com.ashraf.rider.dto;

import com.ashraf.rider.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDeliveryStatusRequest(@NotNull DeliveryStatus status) {}