package com.ashraf.rider.entity;

import com.ashraf.rider.enums.DeliveryStatus;
import com.ashraf.restaurant.core.entity.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery")
@Getter
@Setter
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private Rider rider;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime deliveryDatetime;

    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private String deliveryOtp; // verify handoff to customer
    private BigDecimal distanceKm;
}
