package com.ashraf.shared.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicIdBackfill implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PublicIdBackfill.class);

    private static final List<String> TABLES = List.of(
            "users",
            "restaurants",
            "menu",
            "orders",
            "address_normalized",
            "restaurant_timings",
            "restaurant_onboarding_applications",
            "riders");

    private final JdbcTemplate jdbc;

    public PublicIdBackfill(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    @Override
    public void run(ApplicationArguments args) {
        for(String table : TABLES) {
            int updated = jdbc.update("UPDATE " + table + " SET public_id = UUID_TO_BIN(UUID()) WHERE public_id IS NULL");
            if(updated > 0) {
                log.info("Backfilled public_id for {} row(s) in {}", updated, table);
            }
        }
    }
}
