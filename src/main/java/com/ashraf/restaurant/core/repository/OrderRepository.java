package com.ashraf.restaurant.core.repository;

import com.ashraf.restaurant.core.entity.Order;
import com.ashraf.restaurant.core.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"restaurant"})
    Page<Order> findByCustomer_IdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"restaurant", "items", "deliveryAddress"})
    Optional<Order> findWithDetailsByIdAndCustomer_Id(Long id, Long customerId);

    // Row lock: verify, the webhook and the expiry job can all touch the same order at once.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Query("select o.id from Order o where o.orderStatus = :status and o.createdAt < :cutoff")
    List<Long> findIdsByStatusCreatedBefore(@Param("status") OrderStatus status,
                                            @Param("cutoff") LocalDateTime cutoff);
}