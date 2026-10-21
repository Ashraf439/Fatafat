package com.ashraf.restaurant.core.entity;

import com.ashraf.commerce.enums.PaymentMethod;
import com.ashraf.commerce.enums.PaymentStatus;
import com.ashraf.customer.entity.Customer;
import com.ashraf.customer.entity.CustomerAddress;
import com.ashraf.payment.entity.OrderRefund;
import com.ashraf.restaurant.core.enums.OrderStatus;
import com.ashraf.rider.entity.Delivery;
import com.ashraf.shared.entity.PublicIdEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order extends PublicIdEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.PLACED;

    private LocalDateTime orderDate;

    @Column(name = "order_number")
    private Integer orderNumber;

    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @OneToOne(mappedBy = "order")
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_address_id", nullable = false)
    private CustomerAddress deliveryAddress;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String specialInstructions;
    private String cancelReason;
    private LocalDateTime estimatedDeliveryTime;

    @OneToMany(mappedBy = "order")           // no cascade: refunds are only created through RefundService
    private List<OrderRefund> refunds = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
