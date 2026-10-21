package com.ashraf.customer.dto;

import com.ashraf.restaurant.core.entity.Order;
import com.ashraf.restaurant.core.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Row in the order-history list. */
public record OrderSummaryResponse(
        UUID id,
        Integer orderNumber,
        String status,
        Long restaurantId,
        String restaurantName,
        String restaurantImageUrl,
        int itemCount,
        List<String> itemsPreview,
        BigDecimal totalAmount,
        LocalDateTime placedAt
) {
    private static final int PREVIEW_LINES = 3;

    public static OrderSummaryResponse from(Order o) {
        List<OrderItem> items = o.getItems();
        int count = items.stream().mapToInt(OrderItem::getQuantity).sum();
        List<String> preview = items.stream()
                .limit(PREVIEW_LINES)
                .map(i -> i.getQuantity() + " × " + i.getDishNameSnapshot())
                .toList();
        return new OrderSummaryResponse(
                o.getPublicId(),
                o.getOrderNumber(),
                o.getOrderStatus().name(),
                o.getRestaurant().getId(),
                o.getRestaurant().getName(),
                o.getRestaurant().getImageUrl(),
                count,
                preview,
                o.getTotalAmount(),
                o.getOrderDate() != null ? o.getOrderDate() : o.getCreatedAt());
    }
}
