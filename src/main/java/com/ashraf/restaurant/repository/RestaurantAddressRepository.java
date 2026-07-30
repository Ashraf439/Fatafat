package com.ashraf.restaurant.repository;

import com.ashraf.restaurant.entity.RestaurantAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantAddressRepository extends JpaRepository<RestaurantAddress, Long> {
}
