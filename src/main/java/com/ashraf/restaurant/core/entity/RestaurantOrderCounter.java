package com.ashraf.restaurant.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "restaurant_order_counters",
        uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id", "business_date"}))
@Getter
@Setter
public class RestaurantOrderCounter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "last_number", nullable = false)
    private int lastNumber;
}