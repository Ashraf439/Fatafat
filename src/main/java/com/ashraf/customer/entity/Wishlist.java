package com.ashraf.customer.entity;

import com.ashraf.restaurant.core.entity.Menu;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist", uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "menu_id"}))
@Getter @Setter
public class Wishlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @CreationTimestamp
    private LocalDateTime createdAt;
}