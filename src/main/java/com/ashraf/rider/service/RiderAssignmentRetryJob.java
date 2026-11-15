package com.ashraf.rider.service;

import com.ashraf.restaurant.core.enums.OrderStatus;
import com.ashraf.restaurant.core.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class RiderAssignmentRetryJob {

    private final OrderRepository orderRepository;
    private final RiderMatchingService riderMatchingService;

    public RiderAssignmentRetryJob(OrderRepository orderRepository, RiderMatchingService riderMatchingService) {
        this.orderRepository = orderRepository;
        this.riderMatchingService = riderMatchingService;
    }

    @Scheduled(fixedDelay = 15_000)
    @Transactional
    public void retryUnassignedDeliveries() {
        List<Long> ids = orderRepository.findIdsAwaitingRider(OrderStatus.OUT_FOR_DELIVERY);
        for (Long id : ids) {
            orderRepository.findByIdForUpdate(id).ifPresent(order -> {
                boolean assigned = riderMatchingService.attemptAssign(order);
                if (!assigned) {
                    log.info("No rider available yet for order {}", order.getId());
                }
            });
        }
    }
}