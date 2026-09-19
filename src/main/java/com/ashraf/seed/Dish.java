package com.ashraf.seed;

import com.ashraf.restaurant.core.enums.FoodType;

import java.util.Set;

/**
 * One real dish from the seed catalog. {@code wiki} is the Wikipedia article whose lead
 * photo is used as the dish image.
 */
public record Dish(String name,
                   String wiki,
                   String description,
                   int price,
                   FoodType foodType,
                   String category,
                   int prepMinutes,
                   Set<MealTime> meals,
                   Set<String> tags) {
}
