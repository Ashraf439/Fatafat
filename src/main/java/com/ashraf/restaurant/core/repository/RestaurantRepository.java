package com.ashraf.restaurant.core.repository;

import com.ashraf.core.enums.Status;
import com.ashraf.restaurant.core.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    boolean existsByIdAndOwnerUser_Id(Long id, Long ownerUserId);
    List<Restaurant> findByOwnerUser_Status(Status status);

    Optional<Restaurant> findByOwnerUser_Id(Long ownerUserId);

    boolean existsByOwnerUser_Id(Long ownerUserId);
}