package com.ashraf.controller;

import com.ashraf.dto.LoginRequest;
import com.ashraf.dto.LoginResponse;
import com.ashraf.dto.RegisterRequest;
import com.ashraf.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return  ResponseEntity.ok("Registration successful. " +
                "Awaiting approval if you registered as restaurant/rider.");
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse loginResponse = authService.login(req);
        return ResponseEntity.ok(loginResponse);
    }
}