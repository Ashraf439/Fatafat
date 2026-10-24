package com.ashraf.customer.service;

import com.ashraf.restaurant.core.entity.Order;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

final class OrderSpecs {
    private OrderSpecs() {}

    static Specification<Order> forCustomer(Long customerId) {
        return (root, q, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    static Specification<Order> placedFrom(LocalDate from) {
        if (from == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
    }

    static Specification<Order> placedTo(LocalDate to) {
        if (to == null) return null;
        LocalDateTime end = to.atTime(23, 59, 59);
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), end);
    }

    static Specification<Order> and(Specification<Order>... specs) {
        Specification<Order> result = null;
        for (Specification<Order> s : specs) {
            if (s == null) continue;
            result = (result == null) ? s : result.and(s);
        }
        return result != null ? result : (root, q, cb) -> cb.conjunction();
    }
}