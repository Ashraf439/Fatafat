package com.ashraf.auth.controller;

import com.ashraf.auth.dto.LoginRequest;
import com.ashraf.auth.dto.LoginResponse;
import com.ashraf.auth.dto.ResendVerificationRequest;
import com.ashraf.auth.service.AuthService;
import com.ashraf.auth.service.EmailService;
import com.ashraf.auth.service.LoginResult;
import com.ashraf.auth.service.RefreshTokenResult;
import com.ashraf.auth.service.RefreshTokenService;
import com.ashraf.core.entity.User;
import com.ashraf.customer.dto.CustomerRegisterRequest;
import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.restaurant.core.dto.RestaurantRegisterRequest;
import com.ashraf.restaurant.core.service.RestaurantAuthService;
import com.ashraf.rider.dto.RiderRegisterRequest;
import com.ashraf.shared.exception.InvalidRefreshTokenException;
import com.ashraf.shared.ratelimit.RateLimit;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final int REFRESH_TOKEN_MAX_AGE_DAYS = 30; // must match RefreshTokenService.EXPIRY_DAYS

    @Value("${frontend.restaurant-url:http://localhost:5173}")
    private String frontendUrl;
    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;
    @Value("${cookie.secure:true}")
    private boolean cookieSecure; // set to false only in local http dev via application-dev.properties

    private final AuthService authService;
    private final EmailService emailService;
    private final RestaurantAuthService restaurantAuthService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, EmailService emailService,
                          RestaurantAuthService restaurantAuthService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.emailService = emailService;
        this.restaurantAuthService = restaurantAuthService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("register/customer")
    //@RateLimit(limit = 5, timeWindow = 60)
    public ResponseEntity<Map<String, String>> registerCustomer(@Valid @RequestBody CustomerRegisterRequest req) {
        authService.registerCustomer(req); // already saves + sends the verification token — don't duplicate here
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
        return ResponseEntity.ok(Map.of("message", "Rider registered. Pending background check."));
    }

    @PostMapping("login")
    @RateLimit(limit = 5, timeWindow = 60)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
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

        long expiresAt = jwtExpirationMs / 1000;

        setAccessTokenCookie(response, result.accessToken());
        setRefreshTokenCookie(response, result.rawRefreshToken());

        return ResponseEntity.ok(new LoginResponse(account, expiresAt));
    }

    @PostMapping("refresh")
    public ResponseEntity<Map<String, Long>> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = readCookie(request, REFRESH_TOKEN_COOKIE);
        if (rawRefreshToken == null) {
            throw new InvalidRefreshTokenException();
        }

        RefreshTokenResult result = refreshTokenService.rotate(rawRefreshToken);
        User user = result.user();

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .toList();
        String newAccessToken = authService.issueAccessToken(user, roles);

        setAccessTokenCookie(response, newAccessToken);
        setRefreshTokenCookie(response, result.rawToken());

        return ResponseEntity.ok(Map.of("expiresAt", jwtExpirationMs / 1000));
    }

    @PostMapping("logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = readCookie(request, REFRESH_TOKEN_COOKIE);
        if (rawRefreshToken != null) {
            refreshTokenService.revokeSession(rawRefreshToken);
        }
        clearCookie(response, ACCESS_TOKEN_COOKIE, "/");
        clearCookie(response, REFRESH_TOKEN_COOKIE, "/api/auth");
        return ResponseEntity.ok().build();
    }


    @GetMapping("/me")
    public ResponseEntity<LoginResponse.Account> me(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = principal.getUser();
        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .toList();
        return ResponseEntity.ok(new LoginResponse.Account(
                user.getId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus().name(),
                roles,
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        ));
    }

    @PostMapping("resend-verification")
    @RateLimit(limit = 3, timeWindow = 300)
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

    private void setAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(jwtExpirationMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String rawToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, rawToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/auth") // only sent to auth endpoints — narrows exposure vs "/"
                .maxAge(Duration.ofDays(REFRESH_TOKEN_MAX_AGE_DAYS))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name, String path) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(path)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }
}