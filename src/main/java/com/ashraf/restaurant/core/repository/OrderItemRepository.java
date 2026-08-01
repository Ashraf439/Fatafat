package com.ashraf.restaurant.core.repository;

import com.ashraf.restaurant.core.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
