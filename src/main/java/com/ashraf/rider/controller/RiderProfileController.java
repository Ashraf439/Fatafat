package com.ashraf.rider.controller;

import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.rider.dto.RiderProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rider/profile")
public class RiderProfileController {
    @GetMapping
    public ResponseEntity<RiderProfileResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(RiderProfileResponse.from(principal.getUser().getRider()));
    }
}