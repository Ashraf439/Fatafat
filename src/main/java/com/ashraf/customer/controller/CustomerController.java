package com.ashraf.customer.controller;

import com.ashraf.restaurant.core.dto.CustomerMenuResponse;
import com.ashraf.restaurant.core.dto.RestaurantDetailResponse;
import com.ashraf.restaurant.core.dto.RestaurantSummaryResponse;
import com.ashraf.restaurant.core.service.MenuService;
import com.ashraf.restaurant.core.service.RestaurantDiscoveryService;
import com.ashraf.shared.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public restaurant discovery endpoints (no login required, see SecurityConfig). */
@RestController
@RequestMapping("api/customer")
public class CustomerController {
    private final MenuService menuService;
    private final RestaurantDiscoveryService discoveryService;

    public CustomerController(MenuService menuService, RestaurantDiscoveryService discoveryService) {
        this.menuService = menuService;
        this.discoveryService = discoveryService;
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
    public ResponseEntity<RestaurantDetailResponse> getRestaurant(@PathVariable("restaurantId") Long restaurantId) {
        return ResponseEntity.ok(discoveryService.getDetail(restaurantId));
    }

    @GetMapping("/restaurants/{restaurantId}/menu")
    public ResponseEntity<CustomerMenuResponse> getMenu(@PathVariable("restaurantId") Long restaurantId) {
        return ResponseEntity.ok(menuService.getMenuForCustomer(restaurantId));
    }
}
