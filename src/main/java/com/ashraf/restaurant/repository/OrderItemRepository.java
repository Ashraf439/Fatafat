package com.ashraf.restaurant.repository;

import com.ashraf.restaurant.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
