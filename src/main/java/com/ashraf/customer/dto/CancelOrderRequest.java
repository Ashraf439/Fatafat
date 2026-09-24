package com.ashraf.customer.dto;

import jakarta.validation.constraints.Size;

public record CancelOrderRequest(@Size(max = 255) String reason) {}
