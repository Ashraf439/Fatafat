package com.ashraf.rider.dto;

import com.ashraf.rider.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateDeliveryStatusRequest(@NotNull DeliveryStatus status,
                                          BigDecimal currentLat, BigDecimal currentLng,
                                          String otp, Boolean codCollected) {}