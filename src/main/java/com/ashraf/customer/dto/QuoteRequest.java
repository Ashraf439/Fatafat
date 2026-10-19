package com.ashraf.customer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record QuoteRequest(
        @NotNull UUID restaurantId,
        @NotEmpty @Size(max = 50) @Valid List<OrderLineRequest> items
) {}
