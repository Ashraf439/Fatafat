package com.ashraf.restaurant.onboarding.entity;

import com.ashraf.core.entity.User;
import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant_onboarding_applications")
@Getter
@Setter
public class RestaurantOnboardingApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer attemptNumber;

    // --- restaurant basic details ---
    private String restaurantName;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;

    // --- document IDs ---
    private String fssaiLicense;
    private String gstin;

    // --- bank details ---
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantOnboardingStatus status;

    @Column(length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private User reviewedByAdmin;

    private LocalDateTime reviewedAt;

    @CreationTimestamp
    private LocalDateTime submittedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}