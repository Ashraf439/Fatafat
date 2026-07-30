package com.ashraf.restaurant.onboarding.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OnboardingPaymentRepository extends JpaRepository<OnboardingPayment, Long> {
    Optional<OnboardingPayment> findByRestaurantOnboardingApplication_Id(Long restaurantOnboardingApplicationId);
    Optional<OnboardingPayment> findByOrderId(String orderId);
}