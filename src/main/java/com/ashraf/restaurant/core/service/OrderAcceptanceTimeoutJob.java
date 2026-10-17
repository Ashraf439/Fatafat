package com.ashraf.restaurant.core.service;

import com.ashraf.payment.enums.RefundReason;
import com.ashraf.payment.service.RefundService;
import com.ashraf.restaurant.core.enums.OrderStatus;
import com.ashraf.restaurant.core.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A paid customer must not wait forever on a restaurant that never answers: orders still PLACED after the
 * timeout are cancelled and refunded automatically.
 */
@Slf4j
@Component
public class OrderAcceptanceTimeoutJob {

    private final OrderRepository orderRepository;
    private final RefundService refundService;
    private final long timeoutMinutes;

    public OrderAcceptanceTimeoutJob(OrderRepository orderRepository, RefundService refundService,
                                     @Value("${order.accept-timeout-minutes:10}") long timeoutMinutes) {
        this.orderRepository = orderRepository;
        this.refundService = refundService;
        this.timeoutMinutes = timeoutMinutes;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void autoRejectUnattendedOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Long> ids = orderRepository.findIdsByStatusUpdatedBefore(OrderStatus.PLACED, cutoff);

        for (Long id : ids) {
            orderRepository.findByIdForUpdate(id).ifPresent(order -> {
                // Re-check under the lock: the restaurant may have accepted it a moment ago.
                if (order.getOrderStatus() != OrderStatus.PLACED) return;
                try {
                    // Refund first: if it fails, the order stays PLACED and is retried on the next run,
                    // instead of ending up cancelled with the money still taken.
                    refundService.refundInFull(order, RefundReason.RESTAURANT_CANCELLED);
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    order.setCancelReason("The restaurant didn't respond in time");
                    log.info("Auto-rejected unattended order {}", id);
                } catch (RuntimeException e) {
                    log.error("Could not auto-reject order {}", id, e);
                }
            });
        }
    }
}