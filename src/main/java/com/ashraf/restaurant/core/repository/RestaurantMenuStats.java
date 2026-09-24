package com.ashraf.restaurant.core.repository;

import java.math.BigDecimal;

/** Per-restaurant menu aggregates used to enrich listing cards (see MenuRepository). */
public interface RestaurantMenuStats {
    Long getRestaurantId();
    BigDecimal getMinPrice();
    Double getAvgPrepMinutes();
    Long getItemCount();
    Long getVegCount();
}
