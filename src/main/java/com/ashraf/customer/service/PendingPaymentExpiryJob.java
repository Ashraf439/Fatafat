package com.ashraf.customer.service;

import com.ashraf.commerce.enums.PaymentStatus;
import com.ashraf.restaurant.core.enums.OrderStatus;
import com.ashraf.restaurant.core.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class PendingPaymentExpiryJob {

    private final OrderRepository orderRepository;
    private final long expiryMinutes;

    public PendingPaymentExpiryJob(OrderRepository orderRepository,
                                   @Value("${order.payment.expiry-minutes:15}") long expiryMinutes) {
        this.orderRepository = orderRepository;
        this.expiryMinutes = expiryMinutes;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireUnpaidOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expiryMinutes);
        List<Long> ids = orderRepository.findIdsByStatusCreatedBefore(OrderStatus.PENDING_PAYMENT, cutoff);

        for (Long id : ids) {
            orderRepository.findByIdForUpdate(id).ifPresent(order -> {
                // Re-check under the lock: the customer may have paid while we were looping.
                if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) return;
                order.setOrderStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setCancelReason("Payment was not completed in time");
                log.info("Expired unpaid order {}", id);
            });
        }
    }
}