package com.ashraf.restaurant.core.repository;

import com.ashraf.restaurant.core.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
}
