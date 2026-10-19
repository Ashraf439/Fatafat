package com.ashraf.restaurant.core.dto;

import com.ashraf.restaurant.core.entity.Menu;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class MenuResponse {
    private final UUID id;
    private final String dishName;
    private final String description;
    private final BigDecimal price;
    private final String foodType;
    private final String category;
    private final Integer preparationTimeMinutes;
    private final String imageUrl;

    public MenuResponse(Menu menu) {
        this.id = menu.getPublicId();
        this.dishName = menu.getDishName();
        this.description = menu.getDescription();
        this.price = menu.getPrice();
        this.foodType = menu.getFoodType() != null ? menu.getFoodType().name() : null;
        this.category = menu.getCategory();
        this.preparationTimeMinutes = menu.getPreparationTimeMinutes();
        this.imageUrl = menu.getImageUrl();
    }
}