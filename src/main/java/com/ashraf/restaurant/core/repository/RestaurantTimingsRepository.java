package com.ashraf.restaurant.core.repository;

import com.ashraf.restaurant.core.entity.RestaurantTimings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestaurantTimingsRepository extends JpaRepository<RestaurantTimings, Long> {

    List<RestaurantTimings> findByRestaurant_IdOrderByDayOfWeekAscOpenTimeAsc(Long restaurantId);

    @Modifying
    @Query("delete from RestaurantTimings t where t.restaurant.id = :restaurantId")
    void deleteByRestaurant_Id(@Param("restaurantId") Long restaurantId);
}