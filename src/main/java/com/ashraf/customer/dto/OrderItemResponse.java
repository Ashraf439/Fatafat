package com.ashraf.customer.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long menuId,
        String dishName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String specialInstructions
) {}
