package com.ashraf.payment.repository;

import com.ashraf.commerce.enums.PaymentStatus;
import com.ashraf.payment.entity.OrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {
    Optional<OrderPayment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<OrderPayment> findFirstByOrder_IdAndPaymentStatusOrderByIdDesc(Long orderId, PaymentStatus status);
}