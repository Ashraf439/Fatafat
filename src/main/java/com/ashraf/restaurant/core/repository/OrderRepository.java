package com.ashraf.restaurant.core.repository;

import com.ashraf.restaurant.core.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Restaurant is fetched with the page (to-one, safe to paginate). Items are lazily loaded in
    // batches via hibernate.default_batch_fetch_size instead of a collection join, which would
    // force in-memory pagination.
    @EntityGraph(attributePaths = {"restaurant"})
    Page<Order> findByCustomer_IdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"restaurant", "items", "deliveryAddress"})
    Optional<Order> findWithDetailsByIdAndCustomer_Id(Long id, Long customerId);
}
