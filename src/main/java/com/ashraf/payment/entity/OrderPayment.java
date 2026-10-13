package com.ashraf.payment.entity;

import com.ashraf.commerce.enums.PaymentStatus;
import com.ashraf.restaurant.core.entity.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_payment")
@Getter
@Setter
public class OrderPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One order can have several attempts (retries, expired Razorpay orders).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, unique = true)
    private String razorpayOrderId;

    @Column(unique = true)
    private String razorPayId;

    // Amount sent to Razorpay, in paise. Compared against the webhook amount.
    @Column(nullable = false)
    private long amountPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;
}