package com.ashraf.payment.service;

import com.ashraf.payment.entity.OrderRefund;
import com.ashraf.payment.enums.RefundStatus;
import com.ashraf.payment.repository.OrderRefundRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class RefundRetryJob {

    private final OrderRefundRepository refundRepo;
    private final RefundService refundService;

    public RefundRetryJob(OrderRefundRepository refundRepo, RefundService refundService) {
        this.refundRepo = refundRepo;
        this.refundService = refundService;
    }

    @Scheduled(fixedDelay = 120_000)
    public void run() {
        LocalDateTime now = LocalDateTime.now();

        // Never reached Razorpay (crash or network error): retry, checking for a duplicate first.
        for (OrderRefund r : refundRepo.findByStatusAndRazorpayRefundIdIsNullAndCreatedAtBefore(
                RefundStatus.PENDING, now.minusMinutes(2))) {
            try { refundService.dispatch(r.getId(), true); }
            catch (Exception e) { log.error("Retry of refund {} failed", r.getId(), e); }
        }

        // Reached Razorpay but no webhook yet: ask Razorpay for the outcome.
        for (OrderRefund r : refundRepo.findByStatusAndRazorpayRefundIdIsNotNullAndCreatedAtBefore(
                RefundStatus.PENDING, now.minusMinutes(30))) {
            try { refundService.reconcile(r.getId()); }
            catch (Exception e) { log.error("Reconcile of refund {} failed", r.getId(), e); }
        }
    }
}