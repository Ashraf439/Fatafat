package com.ashraf.customer.dto;

import java.math.BigDecimal;
import java.util.List;

/** Server-priced preview of a cart. The client never computes money; it only displays this. */
public record OrderQuoteResponse(
        Long restaurantId,
        String restaurantName,
        List<Line> lines,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal amountForFreeDelivery,
        BigDecimal minOrderAmount,
        int estimatedDeliveryMinutes
) {
    public record Line(
            Long menuId,
            String dishName,
            String imageUrl,
            String foodType,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ) {}
}
