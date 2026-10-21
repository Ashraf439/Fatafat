package com.ashraf.restaurant.core.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class OrderNumberService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final JdbcTemplate jdbc;

    public OrderNumberService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int next(Long restaurantId) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        jdbc.update("""
                INSERT INTO restaurant_order_counters (restaurant_id, business_date, last_number)
                VALUES (?, ?, 0)
                ON DUPLICATE KEY UPDATE last_number = last_number
                """, restaurantId, today);

        Integer last = jdbc.queryForObject(
                "SELECT last_number FROM restaurant_order_counters WHERE restaurant_id = ? AND business_date = ? FOR UPDATE",
                Integer.class, restaurantId, today);
        int next = (last == null ? 0 : last) + 1;

        jdbc.update("UPDATE restaurant_order_counters SET last_number = ? WHERE restaurant_id = ? AND business_date = ?",
                next, restaurantId, today);
        return next;
    }
}