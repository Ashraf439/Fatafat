package com.ashraf.rider.controller;

import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.rider.dto.AvailabilityRequest;
import com.ashraf.rider.service.RiderAvailabilityService;
import com.ashraf.shared.exception.BusinessRuleException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rider/availability")
public class RiderAvailabilityController {

    private final RiderAvailabilityService availabilityService;

    public RiderAvailabilityController(RiderAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping
    public ResponseEntity<Void> setAvailability(@AuthenticationPrincipal CustomUserDetails principal,
                                                @Valid @RequestBody AvailabilityRequest request) {
        Long riderId = principal.getUser().getRider().getId();
        if (Boolean.TRUE.equals(request.online())) {
            if (request.latitude() == null || request.longitude() == null) {
                throw new BusinessRuleException("Current location is required to go online");
            }
            availabilityService.setOnline(riderId, request.latitude(), request.longitude());
        } else {
            availabilityService.setOffline(riderId);
        }
        return ResponseEntity.ok().build();
    }
}