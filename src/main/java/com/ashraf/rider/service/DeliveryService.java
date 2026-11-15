package com.ashraf.rider.service;

import com.ashraf.core.entity.User;
import com.ashraf.customer.entity.Customer;
import com.ashraf.customer.entity.CustomerAddress;
import com.ashraf.customer.service.CustomerLookupService;
import com.ashraf.restaurant.core.entity.Order;
import com.ashraf.restaurant.core.entity.RestaurantAddress;
import com.ashraf.restaurant.core.enums.OrderStatus;
import com.ashraf.restaurant.core.service.RestaurantAccessService;
import com.ashraf.rider.dto.DeliveryTrackingResponse;
import com.ashraf.rider.entity.Delivery;
import com.ashraf.rider.entity.Rider;
import com.ashraf.rider.enums.DeliveryStatus;
import com.ashraf.rider.repository.DeliveryRepository;
import com.ashraf.shared.exception.BusinessRuleException;
import com.ashraf.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class DeliveryService {

    private static final Map<DeliveryStatus, DeliveryStatus> NEXT = Map.of(
            DeliveryStatus.PENDING, DeliveryStatus.PICKED_UP,
            DeliveryStatus.PICKED_UP, DeliveryStatus.ON_THE_WAY,
            DeliveryStatus.ON_THE_WAY, DeliveryStatus.DELIVERED);

    private final DeliveryRepository deliveryRepository;
    private final CustomerLookupService customerLookup;
    private final RestaurantAccessService restaurantAccess;
    private final RoutingService routingService;

    public DeliveryService(DeliveryRepository deliveryRepository, CustomerLookupService customerLookup,
                           RestaurantAccessService restaurantAccess, RoutingService routingService) {
        this.deliveryRepository = deliveryRepository;
        this.customerLookup = customerLookup;
        this.restaurantAccess = restaurantAccess;
        this.routingService = routingService;
    }

    // ---- rider-side transitions --------------------------------------------------------

    @Transactional
    public Delivery advance(Long riderId, UUID orderPublicId, DeliveryStatus target) {
        Delivery delivery = ownedByRider(riderId, orderPublicId);
        DeliveryStatus current = delivery.getDeliveryStatus();
        if (NEXT.get(current) != target) {
            throw new BusinessRuleException("Delivery is " + current + ", can't move to " + target);
        }
        delivery.setDeliveryStatus(target);
        if (target == DeliveryStatus.PICKED_UP) delivery.setPickedUpAt(LocalDateTime.now());
        if (target == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(LocalDateTime.now());
            delivery.getOrder().setOrderStatus(OrderStatus.DELIVERED);
            delivery.getRider().setIsAvailable(true);
        }
        return delivery;
    }

    @Transactional(readOnly = true)
    public Delivery getActiveDelivery(Long riderId) {
        return deliveryRepository.findFirstByRider_IdAndDeliveryStatusNot(riderId, DeliveryStatus.DELIVERED)
                .orElseThrow(() -> new ResourceNotFoundException("No active delivery"));
    }

    // ---- tracking, for the customer & restaurant apps ----------------------------------

    @Transactional(readOnly = true)
    public DeliveryTrackingResponse trackForCustomer(User user, UUID orderPublicId) {
        Customer customer = customerLookup.requireCustomer(user);
        Delivery delivery = deliveryRepository.findByOrder_PublicId(orderPublicId)
                .filter(d -> d.getOrder().getCustomer().getId().equals(customer.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
        return toTrackingResponse(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryTrackingResponse trackForRestaurant(User user, UUID orderPublicId) {
        Long restaurantId = restaurantAccess.resolveRestaurantId(user);
        Delivery delivery = deliveryRepository.findByOrder_PublicId(orderPublicId)
                .filter(d -> d.getOrder().getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
        return toTrackingResponse(delivery);
    }

    private DeliveryTrackingResponse toTrackingResponse(Delivery delivery) {
        Order order = delivery.getOrder();
        RestaurantAddress pickup = defaultAddress(order);
        CustomerAddress drop = order.getDeliveryAddress();
        Rider rider = delivery.getRider();

        return new DeliveryTrackingResponse(
                delivery.getDeliveryStatus().name(), rider.getName(), rider.getUser().getPhoneNumber(),
                rider.getVehicleType(), rider.getVehicleNumber(),
                pickup != null ? pickup.getLatitude() : null, pickup != null ? pickup.getLongitude() : null,
                drop.getLatitude(), drop.getLongitude(),
                "/topic/delivery/" + order.getPublicId() + "/location");
    }

    // ---- routing, for the rider's map ---------------------------------------------------

    @Transactional(readOnly = true)
    public RoutingService.RouteResult routeFor(Long riderId, UUID orderPublicId, BigDecimal currentLat, BigDecimal currentLng) {
        Delivery delivery = ownedByRider(riderId, orderPublicId);
        Order order = delivery.getOrder();

        BigDecimal destLat, destLng;
        if (delivery.getDeliveryStatus() == DeliveryStatus.PENDING) {
            RestaurantAddress pickup = defaultAddress(order);
            if (pickup == null) throw new IllegalStateException("Restaurant has no address on file");
            destLat = pickup.getLatitude(); destLng = pickup.getLongitude();
        } else {
            CustomerAddress drop = order.getDeliveryAddress();
            destLat = drop.getLatitude(); destLng = drop.getLongitude();
        }
        return routingService.route(currentLat, currentLng, destLat, destLng);
    }

    private Delivery ownedByRider(Long riderId, UUID orderPublicId) {
        return deliveryRepository.findByOrder_PublicId(orderPublicId)
                .filter(d -> d.getRider().getId().equals(riderId))
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
    }

    private RestaurantAddress defaultAddress(Order order) {
        return order.getRestaurant().getAddresses().stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                .findFirst().orElse(null);
    }
}