package com.ashraf.customer.controller;

import com.ashraf.customer.dto.WishlistItemResponse;
import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.customer.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/customer/wishlist")
public class WishlistController {
    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(wishlistService.list(principal.getUser()));
    }

    @PutMapping("/{menuId}")
    public ResponseEntity<Void> add(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID menuId) {
        wishlistService.add(principal.getUser(), menuId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID menuId) {
        wishlistService.remove(principal.getUser(), menuId);
        return ResponseEntity.noContent().build();
    }
}