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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class RiderMatchingService {
    private static final double[] EXPANDING_RADIUS_KM = {2, 5, 10};

    private final RiderLocationService riderLocationService;
    private final RiderRepository riderRepository;
    private final DeliveryRepository deliveryRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${delivery.high-value-otp-threshold:1000}")
    private BigDecimal highValueThreshold;

    public RiderMatchingService(RiderLocationService riderLocationService,
                                RiderRepository riderRepository, DeliveryRepository deliveryRepository, SimpMessagingTemplate messagingTemplate) {
        this.riderLocationService = riderLocationService;
        this.riderRepository = riderRepository;
        this.deliveryRepository = deliveryRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public boolean attemptAssign(Order order) {
        if (deliveryRepository.existsByOrder_Id(order.getId())) return true; // already matched

        RestaurantAddress pickup = order.getRestaurant().getAddresses().stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Restaurant has no address on file"));

        for (double radiusKm : EXPANDING_RADIUS_KM) {
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