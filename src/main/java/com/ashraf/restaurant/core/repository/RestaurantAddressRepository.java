package com.ashraf.restaurant.core.repository;

import com.ashraf.restaurant.core.entity.RestaurantAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantAddressRepository extends JpaRepository<RestaurantAddress, Long> {
}
