package com.ashraf.restaurant.core.repository;

import com.ashraf.core.enums.Status;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    boolean existsByIdAndOwnerUser_Id(Long id, Long ownerUserId);
    List<Restaurant> findByOwnerUser_Status(Status status);

    Optional<Restaurant> findByOwnerUser_Id(Long ownerUserId);

    boolean existsByOwnerUser_Id(Long ownerUserId);

    List<Restaurant> findByRestaurantOnboardingStatus(RestaurantOnboardingStatus status);

    /**
     * Customer-facing discovery query over LIVE restaurants. Empty-string parameters mean
     * "no filter" (avoids typed-null binding problems). {@code query} and {@code city} must
     * already be lower-cased by the caller. Open restaurants are listed first, then by name.
     */
    @Query("""
            select r from Restaurant r
            where r.restaurantOnboardingStatus = com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus.LIVE
              and (:openOnly = false or r.isOpen = true)
              and (:query = '' or lower(r.name) like concat('%', :query, '%'))
              and (:city = '' or exists (
                    select 1 from RestaurantAddress a
                    where a.restaurant = r and lower(a.city) = :city))
              and (:pureVeg = false or (
                    exists (select 1 from Menu m1 where m1.restaurant = r)
                    and not exists (select 1 from Menu m2
                                    where m2.restaurant = r
                                      and m2.foodType <> com.ashraf.restaurant.core.enums.FoodType.VEG)))
            order by r.isOpen desc, r.name asc
            """)
    Page<Restaurant> searchLive(@Param("query") String query,
                                @Param("city") String city,
                                @Param("openOnly") boolean openOnly,
                                @Param("pureVeg") boolean pureVeg,
                                Pageable pageable);
}
