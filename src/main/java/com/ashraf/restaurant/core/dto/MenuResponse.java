package com.ashraf.restaurant.core.dto;

import com.ashraf.restaurant.core.entity.Menu;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class MenuResponse {
    private final Long id;
    private final String dishName;
    private final String description;
    private final BigDecimal price;
    private final String foodType;
    private final String category;
    private final Integer preparationTimeMinutes;

    public MenuResponse(Menu menu) {
        this.id = menu.getId();
        this.dishName = menu.getDishName();
        this.description = menu.getDescription();
        this.price = menu.getPrice();
        this.foodType = menu.getFoodType() != null ? menu.getFoodType().name() : null;
        this.category = menu.getCategory();
        this.preparationTimeMinutes = menu.getPreparationTimeMinutes();
    }
}