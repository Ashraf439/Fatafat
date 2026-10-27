package com.ashraf.customer.repository;

import com.ashraf.customer.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    @EntityGraph(attributePaths = {"menu", "menu.restaurant"})
    List<Wishlist> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    Optional<Wishlist> findByCustomer_IdAndMenu_PublicId(Long customerId, UUID menuPublicId);

    boolean existsByCustomer_IdAndMenu_PublicId(Long customerId, UUID menuPublicId);
}