package com.ashraf.restaurant.core.dto;

import com.ashraf.restaurant.core.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {}