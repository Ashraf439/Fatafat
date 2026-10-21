package com.ashraf.restaurant.core.dto;

import com.ashraf.customer.dto.AddressResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** An order as the restaurant sees it. Contact details stay hidden until the order has been accepted. */
public record RestaurantOrderResponse(
        UUID id,
        Integer orderNumber,
        String status,
        LocalDateTime placedAt,
        LocalDateTime estimatedDeliveryTime,
        String customerName,
        String customerPhone,               // null until accepted
        AddressResponse deliveryAddress,    // null until accepted
        List<Line> items,
        BigDecimal totalAmount,
        String paymentMethod,
        String paymentStatus,
        String specialInstructions,
        String cancelReason,
        String nextStatus,                  // the one status this user may move the order to, or null
        boolean cancellable                 // whether this user may cancel/reject it right now
) {
    public record Line(String name, int quantity, String instructions) {}
}