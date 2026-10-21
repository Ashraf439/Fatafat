package com.ashraf.shared.migration;

import com.ashraf.shared.utils.Slugs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RestaurantSlugBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RestaurantSlugBackfill.class);

    private record Row(long id, String name, UUID publicId) {}

    private final JdbcTemplate jdbc;

    public RestaurantSlugBackfill(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Row> rows = jdbc.query(
                "SELECT id, name, BIN_TO_UUID(public_id) AS public_id FROM restaurants WHERE slug IS NULL AND public_id IS NOT NULL",
                (rs, i) -> new Row(rs.getLong("id"), rs.getString("name"), UUID.fromString(rs.getString("public_id"))));
        for (Row row : rows) {
            jdbc.update("UPDATE restaurants SET slug = ? WHERE id = ?", Slugs.forRestaurant(row.name(), row.publicId()), row.id());
        }
        if (!rows.isEmpty()) {
            log.info("Backfilled slug for {} restaurant(s)", rows.size());
        }
    }
}