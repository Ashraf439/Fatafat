package com.ashraf.customer.dto;

import com.ashraf.commerce.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(
        @NotNull UUID restaurantId,
        @NotNull UUID addressId,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 500) String specialInstructions,
        @NotEmpty @Size(max = 50) @Valid List<OrderLineRequest> items
) {}
