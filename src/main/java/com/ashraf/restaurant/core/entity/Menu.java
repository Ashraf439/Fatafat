package com.ashraf.restaurant.core.entity;

import com.ashraf.restaurant.core.enums.FoodType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;

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
    private Boolean isAvailable;

    @Enumerated(EnumType.STRING)
    private FoodType foodType; // VEG, NON_VEG, EGG — new enum, near-universal in Indian food apps
    private String category; // "Starters", "Main Course", "Desserts"
    private String imageUrl;
    private Integer preparationTimeMinutes;

    @CreationTimestamp
    private java.time.LocalDateTime createdAt;
    @UpdateTimestamp
    private java.time.LocalDateTime updatedAt;
}
