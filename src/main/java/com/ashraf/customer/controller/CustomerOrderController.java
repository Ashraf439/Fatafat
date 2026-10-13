package com.ashraf.customer.controller;

import com.ashraf.customer.dto.*;
import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.customer.service.CustomerOrderService;
import com.ashraf.customer.service.CustomerPaymentService;
import com.ashraf.customer.service.CustomerPaymentService.PaymentSession;
import com.ashraf.payment.dto.VerifyOrderPaymentRequest;
import com.ashraf.shared.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/customer/orders")
public class CustomerOrderController {

    private final CustomerOrderService orderService;
    private final CustomerPaymentService customerPaymentService;

    public CustomerOrderController(CustomerOrderService orderService,
                                   CustomerPaymentService customerPaymentService) {
        this.orderService = orderService;
        this.customerPaymentService = customerPaymentService;
    }

    @PostMapping("/quote")
    public ResponseEntity<OrderQuoteResponse> quote(@Valid @RequestBody QuoteRequest request) {
        return ResponseEntity.ok(orderService.quote(request));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal CustomUserDetails principal,
                                               @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(principal.getUser(), request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.list(principal.getUser(), page, size));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> get(@AuthenticationPrincipal CustomUserDetails principal,
                                             @PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(orderService.get(principal.getUser(), orderId));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(@AuthenticationPrincipal CustomUserDetails principal,
                                                @PathVariable("orderId") Long orderId,
                                                @Valid @RequestBody(required = false) CancelOrderRequest request) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(orderService.cancel(principal.getUser(), orderId, reason));
    }

    @PostMapping("/{orderId}/payment")
    public ResponseEntity<PaymentSession> startPayment(@AuthenticationPrincipal CustomUserDetails principal,
                                                       @PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(customerPaymentService.start(principal.getUser(), orderId));
    }

    @PostMapping("/{orderId}/payment/verify")
    public ResponseEntity<OrderResponse> verifyPayment(@AuthenticationPrincipal CustomUserDetails principal,
                                                       @PathVariable("orderId") Long orderId,
                                                       @Valid @RequestBody VerifyOrderPaymentRequest request) {
        return ResponseEntity.ok(customerPaymentService.confirm(principal.getUser(), orderId, request));
    }
}