package com.ashraf.payment.controller;

import com.ashraf.customer.service.CustomerPaymentService;
import com.ashraf.payment.service.RefundService;
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
    private final RefundService refundService;
    private final String webhookSecret;

    public PaymentWebhookController(CustomerPaymentService customerPayments,
                                    RefundService refundService,
                                    @Value("${razor.webhook.secret:}") String webhookSecret) {
        this.customerPayments = customerPayments;
        this.refundService = refundService;
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
        JSONObject payload = event.optJSONObject("payload");
        if (payload == null) return ResponseEntity.ok().build();

        switch (event.optString("event")) {
            case "payment.captured" -> {
                JSONObject p = entity(payload, "payment");
                if (p != null && !p.optString("order_id").isBlank()
                        && !customerPayments.onCaptured(p.getString("order_id"), p.getString("id"), p.getLong("amount"))) {
                    log.warn("payment.captured for unknown razorpay order {} (onboarding?)", p.getString("order_id"));
                }
            }
            case "payment.failed" -> {
                JSONObject p = entity(payload, "payment");
                if (p != null && !p.optString("order_id").isBlank()) {
                    customerPayments.onFailed(p.getString("order_id"));
                }
            }
            case "refund.processed" -> {
                JSONObject r = entity(payload, "refund");
                if (r != null) refundService.onGatewayRefundEvent(r, true);
            }
            case "refund.failed" -> {
                JSONObject r = entity(payload, "refund");
                if (r != null) refundService.onGatewayRefundEvent(r, false);
            }
            default -> log.debug("Ignoring webhook event {}", event.optString("event"));
        }

        // 2xx for everything we understood. Exceptions above surface as 5xx so Razorpay retries.
        return ResponseEntity.ok().build();
    }

    /** payload.<key>.entity, or null if absent. */
    private static JSONObject entity(JSONObject payload, String key) {
        JSONObject wrapper = payload.optJSONObject(key);
        return wrapper == null ? null : wrapper.optJSONObject("entity");
    }

    private boolean signatureValid(String rawBody, String signature) {
        try {
            return PaymentUtils.verifySignature(rawBody, signature, webhookSecret);
        } catch (Exception e) {
            return false;
        }
    }
}