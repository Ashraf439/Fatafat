package com.ashraf.rider.dto;

import com.ashraf.customer.entity.Customer;
import com.ashraf.customer.entity.CustomerAddress;
import com.ashraf.restaurant.core.entity.Order;
import com.ashraf.restaurant.core.entity.RestaurantAddress;
import com.ashraf.rider.entity.Delivery;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * What the rider app needs for the job in hand. Never carries the delivery OTP:
 * the customer reads it out at the door and the rider types it in.
 */
public record ActiveDeliveryResponse(UUID orderPublicId, String orderCode, String status, String orderStatus,
                                     String restaurantName, String pickupAddress,
                                     BigDecimal pickupLatitude, BigDecimal pickupLongitude,
                                     String dropAddress, BigDecimal dropLatitude, BigDecimal dropLongitude,
                                     String customerName, String customerPhone,
                                     String paymentMethod, BigDecimal totalAmount,
                                     Boolean otpRequired, Boolean codCollected,
                                     String specialInstructions, List<Item> items,
                                     LocalDateTime assignedAt, LocalDateTime pickedUpAt, LocalDateTime deliveredAt) {

    public record Item(String name, Integer quantity, String note) {}

    /** Call inside a transaction: walks lazy order, customer, restaurant and item associations. */
    public static ActiveDeliveryResponse from(Delivery delivery) {
        Order order = delivery.getOrder();
        Customer customer = order.getCustomer();
        CustomerAddress drop = order.getDeliveryAddress();
        RestaurantAddress pickup = order.getRestaurant().getAddresses().stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                .findFirst().orElse(null);

        String phone = customer.getPhoneNumber() != null ? customer.getPhoneNumber()
                : customer.getUser() != null ? customer.getUser().getPhoneNumber() : null;

        List<Item> items = order.getItems() == null ? List.of() : order.getItems().stream()
                .map(i -> new Item(i.getDishNameSnapshot(), i.getQuantity(), i.getSpecialInstructions()))
                .toList();

        return new ActiveDeliveryResponse(
                order.getPublicId(), order.getOrderCode(), delivery.getDeliveryStatus().name(),
                order.getOrderStatus() != null ? order.getOrderStatus().name() : null,
                order.getRestaurant().getName(),
                pickup != null ? join(pickup.getFloorOrApartment(), pickup.getStreet(), pickup.getLandmark(),
                        pickup.getCity(), pickup.getPincode()) : null,
                pickup != null ? pickup.getLatitude() : null, pickup != null ? pickup.getLongitude() : null,
                drop != null ? join(drop.getFloorOrApartment(), drop.getStreet(), drop.getLandmark(),
                        drop.getCity(), drop.getPincode()) : null,
                drop != null ? drop.getLatitude() : null, drop != null ? drop.getLongitude() : null,
                customer.getName(), phone,
                order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null, order.getTotalAmount(),
                Boolean.TRUE.equals(delivery.getOtpRequired()), Boolean.TRUE.equals(delivery.getCodCollected()),
                order.getSpecialInstructions(), items,
                delivery.getDeliveryDatetime(), delivery.getPickedUpAt(), delivery.getDeliveredAt());
    }

    private static String join(String... parts) {
        return Stream.of(parts).filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }
}
