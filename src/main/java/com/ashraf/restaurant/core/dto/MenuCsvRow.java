package com.ashraf.restaurant.core.dto;

import com.opencsv.bean.CsvBindByName;

import java.math.BigDecimal;

public class MenuCsvRow {

    @CsvBindByName(column = "DishName")
    private String dishName;

    @CsvBindByName(column = "Description")
    private String description;

    @CsvBindByName(column = "Price")
    private BigDecimal price;

    @CsvBindByName(column = "FoodType")
    private String foodType;

    @CsvBindByName(column = "Category")
    private String category;

    @CsvBindByName(column = "PreparationTime")
    private Integer preparationTimeMinutes;

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getPreparationTimeMinutes() { return preparationTimeMinutes; }
    public void setPreparationTimeMinutes(Integer preparationTimeMinutes) { this.preparationTimeMinutes = preparationTimeMinutes; }
}