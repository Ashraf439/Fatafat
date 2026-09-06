package com.ashraf.restaurant.core.controller;

import com.ashraf.core.service.PermissionService;
import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.restaurant.core.dto.MenuResponse;
import com.ashraf.restaurant.core.entity.Menu;
import com.ashraf.restaurant.core.service.MenuService;
import com.ashraf.restaurant.core.service.RestaurantService;
import com.ashraf.shared.exception.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurant")
public class RestaurantController {

    private final MenuService menuService;
    private final PermissionService permissionService;
    private final RestaurantService restaurantService;

    public RestaurantController(MenuService menuService, PermissionService permissionService, RestaurantService restaurantService) {
        this.menuService = menuService;
        this.permissionService = permissionService;
        this.restaurantService = restaurantService;
    }

    @PostMapping("/menu-upload")
    public ResponseEntity<Map<String, String>> uploadMenu(@RequestParam("file") MultipartFile file,
                                                          @AuthenticationPrincipal CustomUserDetails principal) {
        if (!permissionService.hasPermission(principal.getUser().getId(), "MENU_ITEM_ADD")) {
            throw new AccessDeniedException("No access");
        }

        if (file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Please upload a valid CSV file."));
        }

        List<Menu> savedMenu = menuService.saveMenu(file, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Successfully saved " + savedMenu.size() + " items."));
    }
    @GetMapping("/menu")
    public ResponseEntity<List<MenuResponse>> getMenu(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(menuService.getMenu(principal.getUser()));
    }
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getStatus(@AuthenticationPrincipal CustomUserDetails principal) {
        boolean isOpen = restaurantService.getStatus(principal.getUser());
        return ResponseEntity.ok(Map.of("isOpen", isOpen));
    }

    @PatchMapping("/status")
    public ResponseEntity<Map<String, Boolean>> toggleStatus(@AuthenticationPrincipal CustomUserDetails principal) {
        boolean isOpen = restaurantService.toggleStatus(principal.getUser());
        return ResponseEntity.ok(Map.of("isOpen", isOpen));
    }
}