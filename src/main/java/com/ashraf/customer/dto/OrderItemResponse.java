package com.ashraf.customer.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID menuId,
        String dishName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String specialInstructions
) {}
