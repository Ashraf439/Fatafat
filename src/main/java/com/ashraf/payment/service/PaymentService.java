package com.ashraf.payment.service;

import com.ashraf.config.RazorpayConfig;
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

    public String createOrder(double amount,String currency) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount * 100);
        orderRequest.put("currency",currency);

        Order order = razorpayClient.orders.create(orderRequest);
        return  order.toString();
    }
    public boolean verifyPayment(String orderId, String paymentId, String razorpaySignature) {
        String payload = orderId + '|' + paymentId;
        try {
            return PaymentUtils.verifySignature(payload, razorpaySignature, razorpayConfig.getApiSecret());
        } catch (Exception e) {
            return false;
        }
    }

}
