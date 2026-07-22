package com.ashraf.controller;

import com.ashraf.dto.*;
import com.ashraf.entity.User;
import com.ashraf.service.AuthService;
import com.ashraf.service.LoginResult;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("register/customer")
    public ResponseEntity<String> registerCustomer(@Valid @RequestBody CustomerRegisterRequest req) {
        authService.registerCustomer(req);
        return ResponseEntity.ok("Customer registered successfully");
    }

    @PostMapping("register/restaurant")
    public ResponseEntity<String> registerRestaurant(@Valid @RequestBody RestaurantRegisterRequest req) {
        authService.registerRestaurant(req);
        return ResponseEntity.ok("Restaurant registered. Pending approval.");
    }

    @PostMapping("register/rider")
    public ResponseEntity<String> registerRider(@Valid @RequestBody RiderRegisterRequest req) {
        authService.registerRider(req);
        return ResponseEntity.ok("Rider registered. Pending background check.");
    }

    @PostMapping("login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletResponse servletResponse) {
        LoginResult result = authService.login(req);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.rawRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(Duration.ofDays(30))
                .sameSite("Strict")
                .build();

        servletResponse.addHeader("Set-Cookie", cookie.toString());

        User user = result.user();

        List<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();

        LoginResponse.UserSummary userSummary = new LoginResponse.UserSummary(
                user.getId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus().name(),
                roles,
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
        LoginResponse body = new LoginResponse(result.accessToken(),userSummary);
        return ResponseEntity.ok(body);
    }
}