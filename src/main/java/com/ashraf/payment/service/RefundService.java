package com.ashraf.payment.service;

import com.ashraf.commerce.enums.PaymentMethod;
import com.ashraf.commerce.enums.PaymentStatus;
import com.ashraf.payment.RefundResult;
import com.ashraf.payment.entity.OrderPayment;
import com.ashraf.payment.entity.OrderRefund;
import com.ashraf.payment.enums.RefundReason;
import com.ashraf.payment.enums.RefundStatus;
import com.ashraf.payment.repository.OrderPaymentRepository;
import com.ashraf.payment.repository.OrderRefundRepository;
import com.ashraf.restaurant.core.entity.Order;
import com.ashraf.restaurant.core.repository.OrderRepository;
import com.ashraf.shared.exception.BusinessRuleException;
import com.ashraf.shared.exception.RefundGatewayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class RefundService {

    public record RefundRequestedEvent(Long refundId) {}

    private static final Set<RefundStatus> ACTIVE = EnumSet.of(RefundStatus.PENDING, RefundStatus.PROCESSED);

    private final PaymentService paymentService;
    private final OrderRefundRepository refundRepo;
    private final OrderPaymentRepository paymentRepo;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher events;

    public RefundService(PaymentService paymentService, OrderRefundRepository refundRepo,
                         OrderPaymentRepository paymentRepo, OrderRepository orderRepository,
                         ApplicationEventPublisher events) {
        this.paymentService = paymentService;
        this.refundRepo = refundRepo;
        this.paymentRepo = paymentRepo;
        this.orderRepository = orderRepository;
        this.events = events;
    }

    // ---- requesting (caller holds the order row lock, inside its own transaction) ----------

    /** Refunds whatever is still refundable on an online-paid order. Returns null if there is nothing to refund. */
    public OrderRefund refundInFull(Order order, RefundReason reason) {
        if (order.getPaymentStatus() != PaymentStatus.PAID || order.getPaymentMethod() != PaymentMethod.ONLINE) {
            return null;
        }
        OrderPayment payment = paymentRepo
                .findFirstByOrder_IdAndPaymentStatusOrderByIdDesc(order.getId(), PaymentStatus.PAID)
                .orElse(null);
        if (payment == null) return null;

        long remaining = payment.getAmountPaise() - refundRepo.sumAmountPaise(order.getId(), ACTIVE);
        return remaining > 0 ? request(order, payment, remaining, reason, null) : null;
    }

    public OrderRefund request(Order order, OrderPayment payment, long amountPaise,
                               RefundReason reason, Long complaintId) {
        long already = refundRepo.sumAmountPaise(order.getId(), ACTIVE);
        if (amountPaise <= 0 || already + amountPaise > payment.getAmountPaise()) {
            throw new BusinessRuleException("Refund amount exceeds what was paid for this order.");
        }

        OrderRefund refund = new OrderRefund();
        refund.setOrder(order);
        refund.setPayment(payment);
        refund.setAmountPaise(amountPaise);
        refund.setReason(reason);
        refund.setComplaintId(complaintId);
        refund = refundRepo.save(refund);
        order.getRefunds().add(refund);

        // Razorpay is only called after this transaction has committed (see onRequested).
        events.publishEvent(new RefundRequestedEvent(refund.getId()));
        return refund;
    }

    // ---- talking to Razorpay -----------------------------------------------------------------

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRequested(RefundRequestedEvent event) {
        dispatch(event.refundId(), false);
    }

    @Transactional
    public void dispatch(Long refundId, boolean retry) {
        OrderRefund refund = refundRepo.findById(refundId).orElse(null);
        if (refund == null || refund.getStatus() != RefundStatus.PENDING || refund.getRazorpayRefundId() != null) return;

        String receipt = "rf_" + refund.getId();
        String paymentId = refund.getPayment().getRazorPayId();
        try {
            RefundResult result = null;
            if (retry) result = paymentService.findRefundByReceipt(paymentId, receipt).orElse(null);
            if (result == null) result = paymentService.refund(paymentId, refund.getAmountPaise(), receipt);
            refund.setRazorpayRefundId(result.refundId());      // PROCESSED is set by the webhook / reconcile
        } catch (RefundGatewayException e) {
            if (e.isDefinitive()) {
                refund.setStatus(RefundStatus.FAILED);
                refund.setFailureReason(e.getMessage());
                log.error("Refund {} rejected by Razorpay: {}", refundId, e.getMessage());
            } else {
                log.warn("Refund {} will be retried: {}", refundId, e.getMessage());
            }
        }
    }

    // ---- settling (webhook and reconciliation) -------------------------------------------------

    @Transactional
    public void onGatewayRefundEvent(JSONObject entity, boolean processed) {
        OrderRefund refund = locate(entity).orElse(null);
        if (refund == null) {
            log.warn("Refund event for unknown refund {}", entity.optString("id"));
            return;
        }
        Order order = orderRepository.findByIdForUpdate(refund.getOrder().getId()).orElseThrow();

        if (refund.getRazorpayRefundId() == null) refund.setRazorpayRefundId(entity.optString("id"));
        if (refund.getStatus() != RefundStatus.PENDING) return;           // already settled: idempotent

        if (processed) {
            refund.setStatus(RefundStatus.PROCESSED);
            refund.setProcessedAt(LocalDateTime.now());
            long refunded = refundRepo.sumAmountPaise(order.getId(), EnumSet.of(RefundStatus.PROCESSED));
            if (refunded >= refund.getPayment().getAmountPaise()) order.setPaymentStatus(PaymentStatus.REFUNDED);
        } else {
            refund.setStatus(RefundStatus.FAILED);
            refund.setFailureReason(entity.optString("error_description", "Refund failed at the gateway"));
            log.error("Refund {} FAILED at Razorpay - needs manual follow-up", refund.getId());
        }
    }

    /** Safety net if a webhook was missed: ask Razorpay directly. */
    @Transactional
    public void reconcile(Long refundId) {
        OrderRefund refund = refundRepo.findById(refundId).orElse(null);
        if (refund == null || refund.getStatus() != RefundStatus.PENDING || refund.getRazorpayRefundId() == null) return;

        String status = paymentService.fetchRefundStatus(refund.getPayment().getRazorPayId(), refund.getRazorpayRefundId());
        if ("processed".equals(status) || "failed".equals(status)) {
            onGatewayRefundEvent(new JSONObject().put("id", refund.getRazorpayRefundId()), "processed".equals(status));
        }
    }

    /** Webhooks can arrive before we've stored Razorpay's id, so fall back to our receipt ("rf_<id>"). */
    private Optional<OrderRefund> locate(JSONObject entity) {
        Optional<OrderRefund> byGatewayId = refundRepo.findByRazorpayRefundId(entity.optString("id"));
        if (byGatewayId.isPresent()) return byGatewayId;
        String receipt = entity.optString("receipt", "");
        if (receipt.startsWith("rf_")) {
            try { return refundRepo.findById(Long.parseLong(receipt.substring(3))); }
            catch (NumberFormatException ignored) { }
        }
        return Optional.empty();
    }
}