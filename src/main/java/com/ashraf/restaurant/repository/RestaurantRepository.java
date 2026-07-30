package com.ashraf.restaurant.repository;

import com.ashraf.restaurant.entity.Restaurant;
import com.ashraf.core.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByOwnerUser_Status(Status status);
}
