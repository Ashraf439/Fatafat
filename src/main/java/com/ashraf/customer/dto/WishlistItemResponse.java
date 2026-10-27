package com.ashraf.customer.dto;

import com.ashraf.customer.entity.Wishlist;

import java.math.BigDecimal;
import java.util.UUID;

public record WishlistItemResponse(
        UUID menuId, String dishName, String imageUrl, BigDecimal price,
        String foodType, UUID restaurantId, String restaurantName, boolean restaurantOpen
) {
    public static WishlistItemResponse from(Wishlist w) {
        var menu = w.getMenu();
        var r = menu.getRestaurant();
        return new WishlistItemResponse(
                menu.getPublicId(), menu.getDishName(), menu.getImageUrl(), menu.getPrice(),
                menu.getFoodType() != null ? menu.getFoodType().name() : null,
                r.getPublicId(), r.getName(), Boolean.TRUE.equals(r.getIsOpen()));
    }
}