package com.ashraf.restaurant.core.repository;

import com.ashraf.restaurant.core.entity.RestaurantAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface RestaurantAddressRepository extends JpaRepository<RestaurantAddress, Long> {

    /** Batch lookup so listing pages never trigger one address query per restaurant. */
    List<RestaurantAddress> findByRestaurant_IdIn(Collection<Long> restaurantIds);

    @Query("""
            select distinct a.city from RestaurantAddress a
            where a.city is not null
              and a.restaurant.restaurantOnboardingStatus = com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus.LIVE
            order by a.city
            """)
    List<String> findLiveCities();
}
