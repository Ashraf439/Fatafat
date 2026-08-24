package com.ashraf.payment.service;

import com.ashraf.config.RazorpayConfig;
import com.ashraf.payment.OrderResult;
import com.ashraf.shared.utils.PaymentUtils;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;

    public PaymentService(RazorpayClient razorpayClient, RazorpayConfig razorpayConfig) {
        this.razorpayClient = razorpayClient;
        this.razorpayConfig = razorpayConfig;
    }

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

    public boolean verifyPayment(String orderId, String paymentId, String razorpaySignature) {

        String payload = orderId + "|" + paymentId;

        try {
            return PaymentUtils.verifySignature(payload, razorpaySignature, razorpayConfig.getApiSecret());
        } catch (Exception e) {
            return false;
        }
    }
}