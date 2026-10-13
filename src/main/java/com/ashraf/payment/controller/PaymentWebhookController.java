package com.ashraf.payment.controller;

import com.ashraf.customer.service.CustomerPaymentService;
import com.ashraf.shared.utils.PaymentUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private final CustomerPaymentService customerPayments;
    private final String webhookSecret;

    public PaymentWebhookController(CustomerPaymentService customerPayments,
                                    @Value("${razor.webhook.secret:}") String webhookSecret) {
        this.customerPayments = customerPayments;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handle(
            @RequestBody byte[] body,   // raw bytes: the signature covers the exact payload, so don't let Jackson touch it
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        String rawBody = new String(body, StandardCharsets.UTF_8);

        if (webhookSecret.isBlank() || signature == null || !signatureValid(rawBody, signature)) {
            log.warn("Rejected webhook with missing/invalid signature");
            return ResponseEntity.status(401).build();
        }

        JSONObject event = new JSONObject(rawBody);
        String type = event.optString("event");

        JSONObject payment = event.optJSONObject("payload") == null ? null
                : event.getJSONObject("payload").optJSONObject("payment") == null ? null
                : event.getJSONObject("payload").getJSONObject("payment").optJSONObject("entity");

        if (payment == null || payment.optString("order_id").isBlank()) {
            return ResponseEntity.ok().build();             // not something we track
        }

        String razorpayOrderId = payment.getString("order_id");

        switch (type) {
            case "payment.captured" -> {
                boolean handled = customerPayments.onCaptured(
                        razorpayOrderId, payment.getString("id"), payment.getLong("amount"));
                if (!handled) {
                    // Not a customer order. Onboarding payments are still confirmed via /api/payments/verify only.
                    log.warn("payment.captured for unknown razorpay order {} (onboarding?)", razorpayOrderId);
                }
            }
            case "payment.failed" -> customerPayments.onFailed(razorpayOrderId);
            default -> log.debug("Ignoring webhook event {}", type);
        }

        // 2xx for everything we understood. Exceptions above surface as 5xx so Razorpay retries.
        return ResponseEntity.ok().build();
    }

    private boolean signatureValid(String rawBody, String signature) {
        try {
            return PaymentUtils.verifySignature(rawBody, signature, webhookSecret);
        } catch (Exception e) {
            return false;
        }
    }
}