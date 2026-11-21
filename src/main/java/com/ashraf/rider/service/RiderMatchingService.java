package com.ashraf.rider.service;

import com.ashraf.customer.entity.CustomerAddress;
import com.ashraf.restaurant.core.entity.Order;
import com.ashraf.restaurant.core.entity.RestaurantAddress;
import com.ashraf.rider.dto.JobOfferResponse;
import com.ashraf.rider.entity.Delivery;
import com.ashraf.rider.entity.Rider;
import com.ashraf.rider.enums.DeliveryStatus;
import com.ashraf.rider.repository.DeliveryRepository;
import com.ashraf.rider.repository.RiderRepository;
import com.ashraf.restaurant.core.enums.OrderStatus;
import com.ashraf.restaurant.core.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
public class RiderMatchingService {

    private final RiderLocationService riderLocationService;
    private final RiderRepository riderRepository;
    private final DeliveryRepository deliveryRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;

    /** Rings searched around the restaurant, nearest first. */
    @Value("${delivery.match-radii-km:2,5,10}")
    private double[] matchRadiiKm;

    @Value("${delivery.high-value-otp-threshold:1000}")
    private BigDecimal highValueThreshold;

    public RiderMatchingService(RiderLocationService riderLocationService,
                                RiderRepository riderRepository, DeliveryRepository deliveryRepository, SimpMessagingTemplate messagingTemplate,
                                OrderRepository orderRepository) {
        this.riderLocationService = riderLocationService;
        this.riderRepository = riderRepository;
        this.deliveryRepository = deliveryRepository;
        this.messagingTemplate = messagingTemplate;
        this.orderRepository = orderRepository;
    }

    /**
     * Loads and locks the order in a transaction of its own, then tries to match it.
     * Used right after the restaurant dispatches an order, once that change has committed.
     */
    @Transactional
    public boolean attemptAssign(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .filter(o -> o.getOrderStatus() == OrderStatus.OUT_FOR_DELIVERY)
                .map(this::attemptAssign)
                .orElse(false);
    }

    @Transactional
    public boolean attemptAssign(Order order) {
        if (deliveryRepository.existsByOrder_Id(order.getId())) return true; // already matched

        RestaurantAddress pickup = order.getRestaurant().getAddresses().stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                .findFirst()
                .orElse(null);
        // Without map coordinates there's nothing to search around. Don't throw: that would fail
        // the restaurant's status change, and the retry job would hit it every run. Log and wait
        // until the restaurant sets its location.
        if (pickup == null || pickup.getLatitude() == null || pickup.getLongitude() == null) {
            log.warn("Order {} can't be matched to a rider: restaurant {} has no map location set",
                    order.getId(), order.getRestaurant().getId());
            return false;
        }
        CustomerAddress drop = order.getDeliveryAddress();
        if (drop == null || drop.getLatitude() == null || drop.getLongitude() == null) {
            log.warn("Order {} can't be matched to a rider: delivery address has no map location", order.getId());
            return false;
        }

        for (double radiusKm : matchRadiiKm) {
            for (Long riderId : riderLocationService.findNearbyRiderIds(
                    pickup.getLatitude(), pickup.getLongitude(), radiusKm, 5)) {
                Optional<Rider> rider = riderRepository.findById(riderId)
                        .filter(r -> Boolean.TRUE.equals(r.getIsAvailable()));
                if (rider.isPresent()) {
                    assign(order, rider.get(), pickup);
                    return true;
                }
            }
        }
        log.info("No available rider within {} km of restaurant {} for order {}",
                matchRadiiKm[matchRadiiKm.length - 1], order.getRestaurant().getId(), order.getId());
        return false;
    }


    private void assign(Order order, Rider rider, RestaurantAddress pickup) {
        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setRider(rider);
        delivery.setDeliveryStatus(DeliveryStatus.PENDING);

        boolean highValue = order.getTotalAmount() != null
                && order.getTotalAmount().compareTo(highValueThreshold) >= 0;
        boolean flaggedAccount = Boolean.TRUE.equals(order.getCustomer().getAlwaysRequireDeliveryOtp());
        boolean otpRequired = highValue || flaggedAccount;

        delivery.setOtpRequired(otpRequired);
        if (otpRequired) {
            delivery.setDeliveryOtp(String.format("%04d", new java.security.SecureRandom().nextInt(10_000)));
        }
        deliveryRepository.save(delivery);

        rider.setIsAvailable(false);
        riderLocationService.markUnavailable(rider.getId());

        CustomerAddress drop = order.getDeliveryAddress();
        messagingTemplate.convertAndSend("/topic/rider/" + rider.getId() + "/jobs",
                new JobOfferResponse(order.getPublicId(), order.getRestaurant().getName(),
                        pickup.getLatitude(), pickup.getLongitude(), drop.getLatitude(), drop.getLongitude()));
    }
}