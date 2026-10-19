package com.ashraf.customer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OrderLineRequest(
        @NotNull UUID menuId,
        @Min(1) @Max(50) int quantity,
        @Size(max = 200) String specialInstructions
) {}
