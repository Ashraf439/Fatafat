package com.ashraf.restaurant.core.repository;

import com.ashraf.restaurant.core.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
