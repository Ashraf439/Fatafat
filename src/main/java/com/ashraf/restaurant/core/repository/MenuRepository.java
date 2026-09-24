package com.ashraf.restaurant.core.repository;

import com.ashraf.restaurant.core.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByRestaurant_Id(Long restaurantId);

    @Query("""
            select m.restaurant.id as restaurantId,
                   min(m.price) as minPrice,
                   avg(m.preparationTimeMinutes) as avgPrepMinutes,
                   count(m) as itemCount,
                   sum(case when m.foodType = com.ashraf.restaurant.core.enums.FoodType.VEG then 1 else 0 end) as vegCount
            from Menu m
            where m.restaurant.id in :restaurantIds
            group by m.restaurant.id
            """)
    List<RestaurantMenuStats> summarizeByRestaurantIds(@Param("restaurantIds") Collection<Long> restaurantIds);
}
