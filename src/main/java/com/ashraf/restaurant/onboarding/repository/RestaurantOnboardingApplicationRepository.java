package com.ashraf.restaurant.onboarding.repository;

import com.ashraf.restaurant.onboarding.entity.RestaurantOnboardingApplication;
import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantOnboardingApplicationRepository
        extends JpaRepository<RestaurantOnboardingApplication, Long> {

    List<RestaurantOnboardingApplication> findByUser_IdOrderByAttemptNumberDesc(Long userId);

    Optional<RestaurantOnboardingApplication> findByUser_IdAndStatusIn(
            Long userId, List<RestaurantOnboardingStatus> statuses);
}