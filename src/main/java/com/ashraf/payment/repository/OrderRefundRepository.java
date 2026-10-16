package com.ashraf.payment.repository;

import com.ashraf.payment.entity.OrderRefund;
import com.ashraf.payment.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRefundRepository extends JpaRepository<OrderRefund, Long> {

    Optional<OrderRefund> findByRazorpayRefundId(String razorpayRefundId);

    @Query("select coalesce(sum(r.amountPaise), 0) from OrderRefund r " +
            "where r.order.id = :orderId and r.status in :statuses")
    long sumAmountPaise(@Param("orderId") Long orderId, @Param("statuses") Collection<RefundStatus> statuses);

    List<OrderRefund> findByStatusAndRazorpayRefundIdIsNullAndCreatedAtBefore(RefundStatus status, LocalDateTime cutoff);

    List<OrderRefund> findByStatusAndRazorpayRefundIdIsNotNullAndCreatedAtBefore(RefundStatus status, LocalDateTime cutoff);
}