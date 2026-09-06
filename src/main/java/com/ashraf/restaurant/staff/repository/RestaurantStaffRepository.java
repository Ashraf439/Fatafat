package com.ashraf.restaurant.staff.repository;

import com.ashraf.restaurant.staff.entity.RestaurantStaff;
import com.ashraf.restaurant.staff.enums.StaffStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantStaffRepository extends JpaRepository<RestaurantStaff, Long> {
    boolean existsByRestaurant_IdAndUser_IdAndStatus(Long restaurantId, Long userId, StaffStatus status);

    List<RestaurantStaff> findByRestaurant_Id(Long restaurantId);

    Optional<RestaurantStaff> findByRestaurant_IdAndUser_Id(Long restaurantId, Long userId);

    Optional<RestaurantStaff> findByUser_IdAndStatus(Long userId, StaffStatus staffStatus);
}