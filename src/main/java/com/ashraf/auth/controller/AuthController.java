package com.ashraf.auth.controller;

import com.ashraf.auth.dto.LoginRequest;
import com.ashraf.auth.dto.LoginResponse;
import com.ashraf.auth.dto.ResendVerificationRequest;
import com.ashraf.auth.service.AuthService;
import com.ashraf.auth.service.EmailService;
import com.ashraf.auth.service.LoginResult;
import com.ashraf.core.entity.User;
import com.ashraf.customer.dto.CustomerRegisterRequest;
import com.ashraf.restaurant.core.dto.RestaurantRegisterRequest;
import com.ashraf.restaurant.core.service.RestaurantAuthService;
import com.ashraf.rider.dto.RiderRegisterRequest;
import com.ashraf.shared.ratelimit.RateLimit;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Value("${frontend.restaurant-url:http://localhost:5173}")
    private String frontendUrl;
    @Value(("${jwt.expiration-ms}"))
    private long jwtExpirationMs;
    private final AuthService authService;
    private final EmailService emailService;
    private final RestaurantAuthService restaurantAuthService;

    public AuthController(AuthService authService, EmailService emailService, RestaurantAuthService restaurantAuthService) {
        this.authService = authService;
        this.emailService = emailService;
        this.restaurantAuthService = restaurantAuthService;
    }

    @PostMapping("register/customer")
    //@RateLimit(limit = 5, timeWindow = 60)
    public ResponseEntity<Map<String, String>> registerCustomer(@Valid @RequestBody CustomerRegisterRequest req) {
        authService.registerCustomer(req);
        String token = UUID.randomUUID().toString();
        emailService.sendVerificationEmail(req.getEmail(), token);
        return ResponseEntity.ok(Map.of("message", "Customer registered successfully"));
    }

    @PostMapping("register/restaurant")
    public ResponseEntity<Map<String, String>> registerRestaurant(@Valid @RequestBody RestaurantRegisterRequest req) {
        restaurantAuthService.registerRestaurant(req);
        return ResponseEntity.ok(Map.of("message", "Restaurant registered. Please verify your email, then track your application status."));
    }

    @PostMapping("register/rider")
    //@RateLimit(limit = 5, timeWindow = 60)
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

    @PostMapping("resend-verification")
    @RateLimit(limit = 3, timeWindow = 300) // adjust once you confirm how RateLimit is keyed
    public ResponseEntity<Map<String, String>> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        authService.resendVerification(req.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "If that email is registered and not yet verified, a new verification link has been sent."
        ));
    }
    @GetMapping("/verify")
    public ResponseEntity<Void> verifyAccount(@RequestParam("token") String token) {
        try {
            authService.verifyEmailToken(token);
            return ResponseEntity.status(302)
                    .location(URI.create(frontendUrl + "?verified=true"))
                    .build();
        } catch (RuntimeException e) {
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return ResponseEntity.status(302)
                    .location(URI.create(frontendUrl + "?verified=false&error=" + msg))
                    .build();
        }
    }
}