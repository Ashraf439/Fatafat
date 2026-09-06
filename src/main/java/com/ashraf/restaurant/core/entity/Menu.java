package com.ashraf.restaurant.core.entity;

import com.ashraf.restaurant.core.enums.FoodType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "menu")
@Getter
@Setter
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    private String dishName;
    private String description;
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private FoodType foodType; // VEG, NON_VEG, EGG — near-universal in Indian food apps
    private String category; // "Starters", "Main Course", "Desserts"
    private String imageUrl;
    private Integer preparationTimeMinutes;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}