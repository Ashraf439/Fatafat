package com.ashraf.customer.controller;

import com.ashraf.restaurant.core.dto.CustomerMenuResponse;
import com.ashraf.restaurant.core.dto.RestaurantSummaryResponse;
import com.ashraf.restaurant.core.service.MenuService;
import com.ashraf.restaurant.core.service.RestaurantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/customer")
public class CustomerController {
    private final MenuService menuService;
    private final RestaurantService restaurantService;
    public CustomerController(MenuService menuService, RestaurantService restaurantService) {
        this.menuService = menuService;
        this.restaurantService = restaurantService;
    }

    @GetMapping("/restaurants/{restaurantId}/menu")
    public ResponseEntity<CustomerMenuResponse>  getMenu(@PathVariable Long restaurantId) {
        CustomerMenuResponse  response = menuService.getMenuForCustomer(restaurantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurants")
    public ResponseEntity<List<RestaurantSummaryResponse>> getRestaurants() {
        List<RestaurantSummaryResponse> response = restaurantService.getRestaurantsForCustomer();
        return ResponseEntity.ok(response);
    }
}
