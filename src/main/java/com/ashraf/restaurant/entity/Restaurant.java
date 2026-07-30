package com.ashraf.restaurant.entity;

import com.ashraf.commerce.entity.BankDetails;
import com.ashraf.core.entity.User;
import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "restaurants")
@Getter
@Setter
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private RestaurantAddress restaurantAddress;

    private String fssaiLicense;

    private String gstin;

    private Boolean isOpen = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Menu> menuItems;

    @OneToOne(mappedBy = "restaurant", cascade = CascadeType.ALL)
    private BankDetails bankDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantOnboardingStatus restaurantOnboardingStatus;

    @PrePersist
    private void onCreate() {
        createdAt = LocalDateTime.now();
    }
}