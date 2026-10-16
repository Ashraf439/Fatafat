package com.ashraf.customer.dto;

import com.ashraf.payment.entity.OrderRefund;
import com.ashraf.payment.enums.RefundStatus;
import com.ashraf.restaurant.core.entity.Order;
import com.ashraf.restaurant.core.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Full order view used by the tracking page and returned when an order is placed. */
public record OrderResponse(
        Long id,
        String status,
        RestaurantRef restaurant,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String paymentMethod,
        String paymentStatus,
        AddressResponse deliveryAddress,
        String specialInstructions,
        String cancelReason,
        LocalDateTime placedAt,
        LocalDateTime estimatedDeliveryTime,
        boolean cancellable,
        RefundInfo refund                       // null when nothing has been refunded
) {
    public record RestaurantRef(Long id, String name, String imageUrl) {}

    public record RefundInfo(BigDecimal amount, String status) {}

    private static RefundInfo refundInfo(Order o) {
        var active = o.getRefunds().stream().filter(r -> r.getStatus() != RefundStatus.FAILED).toList();
        if (active.isEmpty()) return null;
        long paise = active.stream().mapToLong(OrderRefund::getAmountPaise).sum();
        boolean done = active.stream().allMatch(r -> r.getStatus() == RefundStatus.PROCESSED);
        return new RefundInfo(BigDecimal.valueOf(paise, 2), done ? "PROCESSED" : "PENDING");
    }

    public static OrderResponse from(Order o) {
        List<OrderItemResponse> lines = o.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getMenu().getId(),
                        i.getDishNameSnapshot(),
                        i.getQuantity(),
                        i.getPriceAtOrder(),
                        i.getPriceAtOrder().multiply(BigDecimal.valueOf(i.getQuantity())),
                        i.getSpecialInstructions()))
                .toList();

        return new OrderResponse(
                o.getId(),
                o.getOrderStatus().name(),
                new RestaurantRef(o.getRestaurant().getId(), o.getRestaurant().getName(),
                        o.getRestaurant().getImageUrl()),
                lines,
                o.getSubtotal(), o.getDeliveryFee(), o.getTaxAmount(), o.getDiscountAmount(),
                o.getTotalAmount(),
                o.getPaymentMethod() != null ? o.getPaymentMethod().name() : null,
                o.getPaymentStatus() != null ? o.getPaymentStatus().name() : null,
                new AddressResponse(o.getDeliveryAddress()),
                o.getSpecialInstructions(),
                o.getCancelReason(),
                o.getOrderDate() != null ? o.getOrderDate() : o.getCreatedAt(),
                o.getEstimatedDeliveryTime(),
                o.getOrderStatus() == OrderStatus.PLACED || o.getOrderStatus() == OrderStatus.PENDING_PAYMENT,
                refundInfo(o));
    }
}