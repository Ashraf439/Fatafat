package com.ashraf.restaurant.core.controller;

import com.ashraf.core.service.PermissionService;
import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.restaurant.core.dto.MenuResponse;
import com.ashraf.restaurant.core.dto.TimingSlotRequest;
import com.ashraf.restaurant.core.dto.TimingSlotResponse;
import com.ashraf.restaurant.core.entity.Menu;
import com.ashraf.restaurant.core.service.MenuService;
import com.ashraf.restaurant.core.service.RestaurantService;
import com.ashraf.restaurant.core.service.RestaurantTimingsService;
import com.ashraf.shared.exception.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final RestaurantTimingsService restaurantTimingsService;

    public RestaurantController(MenuService menuService, PermissionService permissionService,
                                RestaurantService restaurantService, RestaurantTimingsService restaurantTimingsService) {
        this.menuService = menuService;
        this.permissionService = permissionService;
        this.restaurantService = restaurantService;
        this.restaurantTimingsService = restaurantTimingsService;
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

    @GetMapping("/timings")
    public ResponseEntity<List<TimingSlotResponse>> getTimings(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(restaurantTimingsService.getTimings(principal.getUser()));
    }

    // Full replace of the weekly schedule: send every shift you want to keep,
    // any day left out is treated as closed.
    @PutMapping("/timings")
    public ResponseEntity<List<TimingSlotResponse>> setTimings(@RequestBody List<TimingSlotRequest> slots,
                                                               @AuthenticationPrincipal CustomUserDetails principal) {
        if (!permissionService.hasPermission(principal.getUser().getId(), "RESTAURANT_EDIT")) {
            throw new AccessDeniedException("No access");
        }
        return ResponseEntity.ok(restaurantTimingsService.replaceTimings(principal.getUser(), slots));
    }

    // Add a single shift without resending the whole week.
    @PostMapping("/timings")
    public ResponseEntity<TimingSlotResponse> addTiming(@RequestBody TimingSlotRequest slot,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        if (!permissionService.hasPermission(principal.getUser().getId(), "RESTAURANT_EDIT")) {
            throw new AccessDeniedException("No access");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantTimingsService.addTiming(principal.getUser(), slot));
    }

    // Edit a single existing shift by its own id.
    @PatchMapping("/timings/{timingId}")
    public ResponseEntity<TimingSlotResponse> updateTiming(@PathVariable Long timingId,
                                                           @RequestBody TimingSlotRequest slot,
                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        if (!permissionService.hasPermission(principal.getUser().getId(), "RESTAURANT_EDIT")) {
            throw new AccessDeniedException("No access");
        }
        return ResponseEntity.ok(restaurantTimingsService.updateTiming(principal.getUser(), timingId, slot));
    }

    // Remove a single shift.
    @DeleteMapping("/timings/{timingId}")
    public ResponseEntity<Void> deleteTiming(@PathVariable Long timingId,
                                             @AuthenticationPrincipal CustomUserDetails principal) {
        if (!permissionService.hasPermission(principal.getUser().getId(), "RESTAURANT_EDIT")) {
            throw new AccessDeniedException("No access");
        }
        restaurantTimingsService.deleteTiming(principal.getUser(), timingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadRestaurantImage(@RequestParam("file") MultipartFile file,
                                                                     @AuthenticationPrincipal CustomUserDetails principal) {
        if (!permissionService.hasPermission(principal.getUser().getId(), "RESTAURANT_EDIT")) {
            throw new AccessDeniedException("No access");
        }
        String imageUrl = restaurantService.updateRestaurantImage(principal.getUser(), file);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @PostMapping(value = "/menu/{menuId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuResponse> uploadMenuItemImage(@PathVariable Long menuId,
                                                            @RequestParam("file") MultipartFile file,
                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        if (!permissionService.hasPermission(principal.getUser().getId(), "MENU_ITEM_ADD")) {
            throw new AccessDeniedException("No access");
        }
        return ResponseEntity.ok(menuService.updateMenuItemImage(principal.getUser(), menuId, file));
    }
}