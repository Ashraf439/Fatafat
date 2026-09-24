package com.ashraf.customer.controller;

import com.ashraf.customer.dto.CancelOrderRequest;
import com.ashraf.customer.dto.OrderQuoteResponse;
import com.ashraf.customer.dto.OrderResponse;
import com.ashraf.customer.dto.OrderSummaryResponse;
import com.ashraf.customer.dto.PlaceOrderRequest;
import com.ashraf.customer.dto.QuoteRequest;
import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.customer.service.CustomerOrderService;
import com.ashraf.shared.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/customer/orders")
public class CustomerOrderController {

    private final CustomerOrderService orderService;

    public CustomerOrderController(CustomerOrderService orderService) {
        this.orderService = orderService;
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
}
