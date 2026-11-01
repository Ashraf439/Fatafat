package com.ashraf.customer.controller;

import com.ashraf.customer.dto.CustomerProfileResponse;
import com.ashraf.customer.dto.CustomerProfileUpdateRequest;
import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.customer.service.CustomerProfileService;
import com.ashraf.restaurant.core.dto.CustomerMenuResponse;
import com.ashraf.restaurant.core.dto.RestaurantDetailResponse;
import com.ashraf.restaurant.core.dto.RestaurantSummaryResponse;
import com.ashraf.restaurant.core.service.MenuService;
import com.ashraf.restaurant.core.service.RestaurantDiscoveryService;
import com.ashraf.shared.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/customer")
public class CustomerController {
    private final MenuService menuService;
    private final RestaurantDiscoveryService discoveryService;
    private final CustomerProfileService customerProfileService;

    public CustomerController(MenuService menuService, RestaurantDiscoveryService discoveryService,
                              CustomerProfileService customerProfileService) {
        this.menuService = menuService;
        this.discoveryService = discoveryService;
        this.customerProfileService = customerProfileService;
    }

    @GetMapping("/me/profile")
    public ResponseEntity<CustomerProfileResponse> myProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(customerProfileService.getMyProfile(principal.getUser()));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<CustomerProfileResponse> updateMyProfile(
            @RequestBody CustomerProfileUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(customerProfileService.updateProfile(principal.getUser(), request));
    }

    @GetMapping("/restaurants")
    public ResponseEntity<PageResponse<RestaurantSummaryResponse>> getRestaurants(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "city", defaultValue = "") String city,
            @RequestParam(name = "openOnly", defaultValue = "false") boolean openOnly,
            @RequestParam(name = "pureVeg", defaultValue = "false") boolean pureVeg,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        return ResponseEntity.ok(discoveryService.search(query, city, openOnly, pureVeg, page, size));
    }

    @GetMapping("/restaurants/cities")
    public ResponseEntity<List<String>> getCities() {
        return ResponseEntity.ok(discoveryService.listCities());
    }

    @GetMapping("/restaurants/{restaurantId}")
    public ResponseEntity<RestaurantDetailResponse> getRestaurant(@PathVariable("restaurantId") String restaurantId) {
        return ResponseEntity.ok(discoveryService.getDetail(restaurantId));
    }

    @GetMapping("/restaurants/{restaurantId}/menu")
    public ResponseEntity<CustomerMenuResponse> getMenu(@PathVariable("restaurantId") String restaurantId) {
        return ResponseEntity.ok(menuService.getMenuForCustomer(restaurantId));
    }
}