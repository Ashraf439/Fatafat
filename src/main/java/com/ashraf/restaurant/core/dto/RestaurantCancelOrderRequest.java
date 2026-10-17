package com.ashraf.restaurant.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** orders.cancel_reason is varchar(255) and we prefix it, so keep this short. */
public record RestaurantCancelOrderRequest(@NotBlank @Size(max = 200) String reason) {}