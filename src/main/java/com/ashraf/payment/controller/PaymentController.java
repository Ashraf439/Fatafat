package com.ashraf.payment.controller;

import com.ashraf.payment.service.PaymentService;
import com.razorpay.RazorpayException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/createOrder")
    public String createOrder(double amount, String currency) throws RazorpayException {
        return paymentService.createOrder(amount, currency);
    }

    @PostMapping("/verify")
    public ResponseEntity verifyPayment(@RequestParam String orderId,
                                        @RequestParam String paymentId,
                                        @RequestParam String razorpaySignature) {
        try {
            boolean isValid = paymentService.verifyPayment(orderId, paymentId, razorpaySignature);
            if (isValid) {
                return ResponseEntity.ok("Payment verified successfully");
            } else {
                return ResponseEntity.status(400).body("Payment verification failed");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error verifying payment");
        }
    }

}
