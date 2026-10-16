package com.ashraf.customer.service;

import com.ashraf.commerce.enums.PaymentMethod;
import com.ashraf.commerce.enums.PaymentStatus;
import com.ashraf.config.RazorpayConfig;
import com.ashraf.core.entity.User;
import com.ashraf.customer.dto.OrderResponse;
import com.ashraf.customer.entity.Customer;
import com.ashraf.payment.OrderResult;
import com.ashraf.payment.dto.VerifyOrderPaymentRequest;
import com.ashraf.payment.entity.OrderPayment;
import com.ashraf.payment.enums.RefundReason;
import com.ashraf.payment.repository.OrderPaymentRepository;
import com.ashraf.payment.service.PaymentService;
import com.ashraf.payment.service.RefundService;
import com.ashraf.restaurant.core.entity.Order;
import com.ashraf.restaurant.core.enums.OrderStatus;
import com.ashraf.restaurant.core.repository.OrderRepository;
import com.ashraf.shared.exception.BusinessRuleException;
import com.ashraf.shared.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class CustomerPaymentService {

    public record PaymentSession(String razorpayOrderId, long amount, String currency, String keyId) {}

    private final PaymentService paymentService;
    private final RazorpayConfig razorpayConfig;
    private final OrderRepository orderRepository;
    private final OrderPaymentRepository paymentRepo;
    private final CustomerLookupService customerLookup;
    private final RefundService refundService;

    public CustomerPaymentService(PaymentService paymentService,
                                  RazorpayConfig razorpayConfig,
                                  OrderRepository orderRepository,
                                  OrderPaymentRepository paymentRepo,
                                  CustomerLookupService customerLookup, RefundService refundService) {
        this.paymentService = paymentService;
        this.razorpayConfig = razorpayConfig;
        this.orderRepository = orderRepository;
        this.paymentRepo = paymentRepo;
        this.customerLookup = customerLookup;
        this.refundService = refundService;
    }

    // ---- customer-facing ---------------------------------------------------------------

    @Transactional
    public PaymentSession start(User user, Long orderId) {
        Order order = ownedOrderForUpdate(user, orderId);
        if (order.getPaymentMethod() != PaymentMethod.ONLINE
                || order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("This order isn't awaiting payment.");
        }

        long paise = toPaise(order.getTotalAmount());

        // Reuse an open attempt so retries don't create a new Razorpay order every time.
        OrderPayment attempt = paymentRepo
                .findFirstByOrder_IdAndPaymentStatusOrderByIdDesc(orderId, PaymentStatus.PENDING)
                .orElse(null);

        if (attempt == null) {
            OrderResult r = paymentService.createOrder(paise, "order_" + orderId);
            attempt = new OrderPayment();
            attempt.setOrder(order);
            attempt.setRazorpayOrderId(r.orderId());
            attempt.setAmountPaise(paise);
            attempt.setPaymentStatus(PaymentStatus.PENDING);
            attempt = paymentRepo.save(attempt);
        }

        return new PaymentSession(attempt.getRazorpayOrderId(), paise, "INR", razorpayConfig.getApiKey());
    }

    @Transactional
    public OrderResponse confirm(User user, Long orderId, VerifyOrderPaymentRequest req) {
        Order order = ownedOrderForUpdate(user, orderId);

        OrderPayment payment = paymentRepo.findByRazorpayOrderId(req.razorpayOrderId())
                .filter(p -> p.getOrder().getId().equals(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!paymentService.verifyPayment(
                req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature())) {
            throw new BusinessRuleException("Payment verification failed.");
        }

        markPaid(order, payment, req.razorpayPaymentId());
        return OrderResponse.from(order);
    }

    // ---- webhook-facing ----------------------------------------------------------------

    /** @return true if this Razorpay order belongs to a customer order (so the caller can stop looking). */
    @Transactional
    public boolean onCaptured(String razorpayOrderId, String razorpayPaymentId, long amountPaise) {
        Optional<OrderPayment> found = paymentRepo.findByRazorpayOrderId(razorpayOrderId);
        if (found.isEmpty()) return false;               // maybe an onboarding payment

        OrderPayment payment = found.get();
        Order order = orderRepository.findByIdForUpdate(payment.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (amountPaise != payment.getAmountPaise()) {
            log.error("Amount mismatch for razorpay order {}: expected {} got {}",
                    razorpayOrderId, payment.getAmountPaise(), amountPaise);
            return true;                                 // handled, but do not mark paid
        }
        markPaid(order, payment, razorpayPaymentId);
        return true;
    }

    /** Razorpay lets the customer retry inside the same popup, so a failure event is only logged. */
    public void onFailed(String razorpayOrderId) {
        log.info("Payment attempt failed for razorpay order {}", razorpayOrderId);
    }

    // ---- internals ---------------------------------------------------------------------

    /** Shared by verify and the webhook. Idempotent: the order row is locked, so the guard is race-safe. */
    private void markPaid(Order order, OrderPayment payment, String razorpayPaymentId) {
        if (order.getPaymentStatus() == PaymentStatus.PAID) return;

        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setRazorPayId(razorpayPaymentId);
        payment.setPaidAt(LocalDateTime.now());
        order.setPaymentStatus(PaymentStatus.PAID);

        if (order.getOrderStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setOrderStatus(OrderStatus.PLACED);    // only now does the restaurant see it
        } else if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            // The order expired before the money arrived: give it straight back.
            log.warn("Payment {} captured for cancelled order {} - refunding", razorpayPaymentId, order.getId());
            refundService.request(order, payment, payment.getAmountPaise(), RefundReason.LATE_PAYMENT, null);
        }
    }

    private Order ownedOrderForUpdate(User user, Long orderId) {
        Customer customer = customerLookup.requireCustomer(user);
        return orderRepository.findByIdForUpdate(orderId)
                .filter(o -> o.getCustomer().getId().equals(customer.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private static long toPaise(BigDecimal rupees) {
        return rupees.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}