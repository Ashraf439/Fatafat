package com.ashraf.payment.service;

import com.ashraf.config.RazorpayConfig;
import com.ashraf.payment.OrderResult;
import com.ashraf.payment.RefundResult;
import com.ashraf.shared.exception.BusinessRuleException;
import com.ashraf.shared.exception.RefundGatewayException;
import com.ashraf.shared.utils.PaymentUtils;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;

    public PaymentService(RazorpayClient razorpayClient, RazorpayConfig razorpayConfig) {
        this.razorpayClient = razorpayClient;
        this.razorpayConfig = razorpayConfig;
    }

    /** Restaurant onboarding fee (unchanged). */
    public OrderResult createOrder(Long applicationId, double amount) throws RazorpayException {

        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        JSONObject orderRequest = new JSONObject();

        long amountInPaise = Math.round(amount * 100);

        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");

        Order order = razorpayClient.orders.create(orderRequest);

        Object rawOrderId = order.get("id");
        String orderId = rawOrderId.toString();

        Object rawAmount = order.get("amount");
        long razorpayAmount = ((Number) rawAmount).longValue();

        return new OrderResult(orderId, razorpayAmount);
    }

    /** Customer food orders. Takes paise directly; never pass floating-point money. */
    public OrderResult createOrder(long amountInPaise, String receipt) {

        if (amountInPaise <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        // Razorpay rejects receipts longer than 40 characters.
        String safeReceipt = receipt.length() > 40 ? receipt.substring(0, 40) : receipt;

        try {
            JSONObject req = new JSONObject()
                    .put("amount", amountInPaise)
                    .put("currency", "INR")
                    .put("receipt", safeReceipt);

            Order o = razorpayClient.orders.create(req);
            return new OrderResult(o.get("id").toString(), ((Number) o.get("amount")).longValue());
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for receipt {}", safeReceipt, e);
            throw new BusinessRuleException("We couldn't start the payment. Please try again in a moment.");
        }
    }

    public boolean verifyPayment(String orderId, String paymentId, String razorpaySignature) {

        String payload = orderId + "|" + paymentId;

        try {
            return PaymentUtils.verifySignature(payload, razorpaySignature, razorpayConfig.getApiSecret());
        } catch (Exception e) {
            return false;
        }
    }
    public RefundResult refund(String razorpayPaymentId, long amountPaise, String receipt) {
        try {
            JSONObject req = new JSONObject()
                    .put("amount", amountPaise)
                    .put("speed", "normal")          // free; instant refunds carry a fee
                    .put("receipt", receipt);
            Refund r = razorpayClient.payments.refund(razorpayPaymentId, req);
            return new RefundResult(r.get("id").toString(), String.valueOf(r.get("status")));
        } catch (RazorpayException e) {
            throw gatewayError(e);
        }
    }

    public Optional<RefundResult> findRefundByReceipt(String razorpayPaymentId, String receipt) {
        try {
            for (Refund r : razorpayClient.payments.fetchAllRefunds(razorpayPaymentId)) {
                JSONObject json = r.toJson();
                if (receipt.equals(json.optString("receipt", null))) {
                    return Optional.of(new RefundResult(json.getString("id"), json.optString("status")));
                }
            }
            return Optional.empty();
        } catch (RazorpayException e) {
            throw gatewayError(e);
        }
    }

    public String fetchRefundStatus(String razorpayPaymentId, String razorpayRefundId) {
        try {
            return razorpayClient.payments.fetchRefund(razorpayPaymentId, razorpayRefundId)
                    .toJson().optString("status");
        } catch (RazorpayException e) {
            throw gatewayError(e);
        }
    }

    private static RefundGatewayException gatewayError(RazorpayException e) {
        int code = e.getStatusCode();
        boolean definitive = code >= 400 && code < 500 && code != 429;
        String message = e.getDescription() != null ? e.getDescription() : e.getMessage();
        return new RefundGatewayException(message, definitive, e);
    }
}