package com.ashraf.restaurant.core.controller;

import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.restaurant.core.dto.RestaurantCancelOrderRequest;
import com.ashraf.restaurant.core.dto.RestaurantOrderResponse;
import com.ashraf.restaurant.core.dto.UpdateOrderStatusRequest;
import com.ashraf.restaurant.core.service.RestaurantOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Covered by the existing /api/restaurant/** role rule in SecurityConfig; permissions are checked in the service. */
@RestController
@RequestMapping("/api/restaurant/orders")
public class RestaurantOrderController {

    private final RestaurantOrderService orderService;

    public RestaurantOrderController(RestaurantOrderService orderService) {
        this.orderService = orderService;
    }

    /** view=active (default): the kitchen queue. view=history: last 50 delivered/cancelled. */
    @GetMapping
    public ResponseEntity<List<RestaurantOrderResponse>> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(name = "view", defaultValue = "active") String view) {
        return ResponseEntity.ok(orderService.list(principal.getUser(), view));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<RestaurantOrderResponse> updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.advance(principal.getUser(), orderId, request.status()));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<RestaurantOrderResponse> cancel(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody RestaurantCancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancel(principal.getUser(), orderId, request.reason()));
    }
}