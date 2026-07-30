package com.ashraf.restaurant.onboarding.payment;

import com.ashraf.restaurant.onboarding.entity.RestaurantOnboardingApplication;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding_payment")
@Getter
@Setter
public class OnboardingPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false, unique = true)
    private RestaurantOnboardingApplication restaurantOnboardingApplication;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column
    private String paymentReference;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OnboardingPaymentStatus onboardingPaymentStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime paidAt;

}
