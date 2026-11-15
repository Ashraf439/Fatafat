package com.ashraf.rider.controller;

import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.rider.dto.UpdateDeliveryStatusRequest;
import com.ashraf.rider.entity.Delivery;
import com.ashraf.rider.service.DeliveryService;
import com.ashraf.rider.service.RoutingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/rider/deliveries")
public class RiderOrderController {

    private final DeliveryService deliveryService;

    public RiderOrderController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/active")
    public ResponseEntity<Delivery> active(@AuthenticationPrincipal CustomUserDetails principal) {
        Long riderId = principal.getUser().getRider().getId();
        return ResponseEntity.ok(deliveryService.getActiveDelivery(riderId));
    }

    @GetMapping("/{orderId}/route")
    public ResponseEntity<RoutingService.RouteResult> route(@AuthenticationPrincipal CustomUserDetails principal,
                                                            @PathVariable UUID orderId,
                                                            @RequestParam BigDecimal currentLat,
                                                            @RequestParam BigDecimal currentLng) {
        Long riderId = principal.getUser().getRider().getId();
        return ResponseEntity.ok(deliveryService.routeFor(riderId, orderId, currentLat, currentLng));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Delivery> updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateDeliveryStatusRequest request) {
        Long riderId = principal.getUser().getRider().getId();
        return ResponseEntity.ok(deliveryService.advance(riderId, orderId, request.status()));
    }
}