package com.ashraf.core.entity;

import com.ashraf.core.enums.Status;
import com.ashraf.customer.entity.Customer;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.rider.entity.Rider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Relationships
    @OneToOne(mappedBy = "user")
    private Customer customer;

    @OneToOne(mappedBy = "user")
    private Rider rider;

    @OneToMany(mappedBy = "ownerUser", fetch = FetchType.LAZY)
    private List<Restaurant> restaurants;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserRoles> userRoles;
}
