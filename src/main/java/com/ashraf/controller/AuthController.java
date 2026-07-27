package com.ashraf.controller;

import com.ashraf.annotation.RateLimit;
import com.ashraf.dto.*;
import com.ashraf.entity.User;
import com.ashraf.service.AuthService;
import com.ashraf.service.LoginResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Value(("${jwt.expiration-ms}"))
    private long jwtExpirationMs;
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("register/customer")
    @RateLimit(limit = 5, timeWindow = 60)
    public ResponseEntity<Map<String, String>> registerCustomer(@Valid @RequestBody CustomerRegisterRequest req) {
        authService.registerCustomer(req);
        return ResponseEntity.ok(Map.of("message", "Customer registered successfully"));
    }

    @PostMapping("register/restaurant")
    @RateLimit(limit = 5, timeWindow = 60)
    public ResponseEntity<Map<String, String>> registerRestaurant(@Valid @RequestBody RestaurantRegisterRequest req) {
        authService.registerRestaurant(req);
        return ResponseEntity.ok(Map.of("message","Restaurant registered. Pending approval."));
    }

    @PostMapping("register/rider")
    @RateLimit(limit = 5, timeWindow = 60)
    public ResponseEntity<Map<String, String>> registerRider(@Valid @RequestBody RiderRegisterRequest req) {
        authService.registerRider(req);
        return ResponseEntity.ok(Map.of("message","Rider registered. Pending background check."));
    }

    @PostMapping("login")
    @RateLimit(limit = 5, timeWindow = 60)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResult result = authService.login(req);
        User user = result.user();

        List<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();

        LoginResponse.Account account = new LoginResponse.Account(
                user.getId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus().name(),
                roles,
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );

        // expiresAt as epoch seconds when the access token expires — adjust if
        // your frontend expects "seconds until expiry" (86400) instead of a
        // timestamp; your example's 86400 value reads like the former (duration),
        // not an absolute epoch time. Confirm which one your owner actually wants.
        long expiresAt =  (jwtExpirationMs / 1000);

        LoginResponse.Tokens tokens = new LoginResponse.Tokens(
                result.accessToken(),
                result.rawRefreshToken(),
                expiresAt
        );

        return ResponseEntity.ok(new LoginResponse(account, tokens));
    }
}