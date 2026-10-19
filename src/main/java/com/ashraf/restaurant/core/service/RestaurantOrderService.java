package com.ashraf.restaurant.core.service;

import com.ashraf.commerce.enums.PaymentMethod;
import com.ashraf.commerce.enums.PaymentStatus;
import com.ashraf.core.entity.User;
import com.ashraf.core.service.PermissionService;
import com.ashraf.customer.dto.AddressResponse;
import com.ashraf.payment.enums.RefundReason;
import com.ashraf.payment.service.RefundService;
import com.ashraf.restaurant.core.dto.RestaurantOrderResponse;
import com.ashraf.restaurant.core.entity.Order;
import com.ashraf.restaurant.core.enums.OrderStatus;
import com.ashraf.restaurant.core.repository.OrderRepository;
import com.ashraf.shared.exception.AccessDeniedException;
import com.ashraf.shared.exception.BusinessRuleException;
import com.ashraf.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.ashraf.restaurant.core.enums.OrderStatus.*;

@Service
public class RestaurantOrderService {

    private static final List<OrderStatus> QUEUE = List.of(PLACED, ACCEPTED, PREPARING, OUT_FOR_DELIVERY);
    private static final List<OrderStatus> FINISHED = List.of(DELIVERED, CANCELLED);

    /** The only legal forward moves. Anything else (skipping a step, moving backwards) is rejected. */
    private static final Map<OrderStatus, OrderStatus> NEXT = Map.of(
            PLACED, ACCEPTED,
            ACCEPTED, PREPARING,
            PREPARING, OUT_FOR_DELIVERY,
            OUT_FOR_DELIVERY, DELIVERED);

    /** Once the food has left the kitchen it can't be cancelled here; that becomes a complaint. */
    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(PLACED, ACCEPTED, PREPARING);

    private static final Set<OrderStatus> CONTACT_VISIBLE = EnumSet.of(ACCEPTED, PREPARING, OUT_FOR_DELIVERY, DELIVERED);

    private final OrderRepository orderRepository;
    private final RestaurantAccessService access;
    private final PermissionService permissions;
    private final RefundService refundService;

    public RestaurantOrderService(OrderRepository orderRepository, RestaurantAccessService access,
                                  PermissionService permissions, RefundService refundService) {
        this.orderRepository = orderRepository;
        this.access = access;
        this.permissions = permissions;
        this.refundService = refundService;
    }

    // ---- reading -----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<RestaurantOrderResponse> list(User user, String view) {
        Long restaurantId = access.resolveRestaurantId(user);
        boolean canAccept = can(user, "ORDER_ACCEPT");
        boolean canCancel = can(user, "ORDER_CANCEL");

        List<Order> orders = "history".equalsIgnoreCase(view)
                ? orderRepository.findByRestaurant_IdAndOrderStatusInOrderByCreatedAtDesc(
                restaurantId, FINISHED, PageRequest.of(0, 50))
                : orderRepository.findQueueForRestaurant(restaurantId, QUEUE);

        return orders.stream().map(o -> toResponse(o, canAccept, canCancel)).toList();
    }

    // ---- moving an order forward -------------------------------------------------------------

    @Transactional
    public RestaurantOrderResponse advance(User user, UUID orderId, OrderStatus target) {
        requirePermission(user, "ORDER_ACCEPT");
        Order order = lockOwnedOrder(user, orderId);

        OrderStatus current = order.getOrderStatus();
        if (NEXT.get(current) != target) {
            throw new BusinessRuleException("This order is " + label(current)
                    + " and can't be moved to " + label(target) + ".");
        }

        order.setOrderStatus(target);
        if (target == DELIVERED && order.getPaymentMethod() == PaymentMethod.CASH) {
            order.setPaymentStatus(PaymentStatus.PAID);          // the cash was collected at the door
        }
        return toResponse(order, true, can(user, "ORDER_CANCEL"));
    }

    // ---- cancelling / rejecting --------------------------------------------------------------

    @Transactional
    public RestaurantOrderResponse cancel(User user, UUID orderId, String reason) {
        requirePermission(user, "ORDER_CANCEL");
        Order order = lockOwnedOrder(user, orderId);

        if (!CANCELLABLE.contains(order.getOrderStatus())) {
            throw new BusinessRuleException("This order is " + label(order.getOrderStatus())
                    + " and can no longer be cancelled.");
        }

        order.setOrderStatus(CANCELLED);
        order.setCancelReason("Cancelled by restaurant: " + reason.trim());

        // Online + paid -> automatic refund. Cash and unpaid orders have nothing to refund.
        refundService.refundInFull(order, RefundReason.RESTAURANT_CANCELLED);

        return toResponse(order, can(user, "ORDER_ACCEPT"), true);
    }

    // ---- internals ---------------------------------------------------------------------------

    /** Locks the row, and only hands it over if it belongs to this user's restaurant. */
    private Order lockOwnedOrder(User user, UUID orderId) {
        Long restaurantId = access.resolveRestaurantId(user);
        return orderRepository.findByPublicIdForUpdate(orderId)
                .filter(o -> o.getRestaurant().getId().equals(restaurantId))
                // An unpaid order doesn't exist as far as the restaurant is concerned.
                .filter(o -> o.getOrderStatus() != PENDING_PAYMENT)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private RestaurantOrderResponse toResponse(Order o, boolean canAccept, boolean canCancel) {
        OrderStatus s = o.getOrderStatus();
        boolean reveal = CONTACT_VISIBLE.contains(s);
        OrderStatus next = canAccept ? NEXT.get(s) : null;

        return new RestaurantOrderResponse(
                o.getPublicId(),
                s.name(),
                o.getOrderDate() != null ? o.getOrderDate() : o.getCreatedAt(),
                o.getEstimatedDeliveryTime(),
                o.getCustomer().getName(),
                reveal ? o.getCustomer().getPhoneNumber() : null,
                reveal ? new AddressResponse(o.getDeliveryAddress()) : null,
                o.getItems().stream()
                        .map(i -> new RestaurantOrderResponse.Line(
                                i.getDishNameSnapshot(), i.getQuantity(), i.getSpecialInstructions()))
                        .toList(),
                o.getTotalAmount(),
                o.getPaymentMethod() != null ? o.getPaymentMethod().name() : null,
                o.getPaymentStatus() != null ? o.getPaymentStatus().name() : null,
                o.getSpecialInstructions(),
                o.getCancelReason(),
                next != null ? next.name() : null,
                canCancel && CANCELLABLE.contains(s));
    }

    private boolean can(User user, String permission) {
        return permissions.hasPermission(user.getId(), permission);
    }

    private void requirePermission(User user, String permission) {
        if (!can(user, permission)) throw new AccessDeniedException("No access");
    }

    private static String label(OrderStatus s) {
        return s.name().toLowerCase().replace('_', ' ');
    }
}