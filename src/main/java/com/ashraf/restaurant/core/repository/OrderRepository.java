package com.ashraf.restaurant.core.repository;

import com.ashraf.restaurant.core.entity.Order;
import com.ashraf.restaurant.core.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // ---- existing customer-side queries: keep exactly as they are in your file -------------
    @EntityGraph(attributePaths = {"restaurant"})
    Page<Order> findByCustomer_IdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"restaurant", "items", "deliveryAddress"})
    Optional<Order> findWithDetailsByIdAndCustomer_Id(Long id, Long customerId);

    // ---- from the payment work ---------------------------------------------------------------
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Query("select o.id from Order o where o.orderStatus = :status and o.createdAt < :cutoff")
    List<Long> findIdsByStatusCreatedBefore(@Param("status") OrderStatus status,
                                            @Param("cutoff") LocalDateTime cutoff);

    // ---- new: restaurant side ----------------------------------------------------------------

    /** The kitchen queue, oldest first so the longest-waiting order is always on top. */
    @EntityGraph(attributePaths = {"customer", "deliveryAddress", "items"})
    @Query("select o from Order o where o.restaurant.id = :restaurantId and o.orderStatus in :statuses " +
            "order by o.createdAt asc")
    List<Order> findQueueForRestaurant(@Param("restaurantId") Long restaurantId,
                                       @Param("statuses") Collection<OrderStatus> statuses);

    /** Finished orders, newest first (limit via Pageable). */
    List<Order> findByRestaurant_IdAndOrderStatusInOrderByCreatedAtDesc(
            Long restaurantId, Collection<OrderStatus> statuses, Pageable pageable);

    /** PLACED orders nobody has touched since {@code cutoff}: updatedAt is when they became PLACED. */
    @Query("select o.id from Order o where o.orderStatus = :status and o.updatedAt < :cutoff")
    List<Long> findIdsByStatusUpdatedBefore(@Param("status") OrderStatus status,
                                            @Param("cutoff") LocalDateTime cutoff);
}