package com.ashraf.repository;

import com.ashraf.entity.Restaurant;
import com.ashraf.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByOwnerUser_Status(Status status);
}
