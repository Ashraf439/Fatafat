package com.ashraf.seed;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

/** Everything needed to create one seeded restaurant, generated deterministically up front. */
public record RestaurantBlueprint(
        int index,
        String cuisine,
        String ownerEmail,
        String ownerName,
        String restaurantName,
        String city,
        String state,
        String locality,
        String street,
        String pincode,
        BigDecimal latitude,
        BigDecimal longitude,
        String landmark,
        String fssai,
        String gstin,
        String bankName,
        String ifsc,
        String accountNumber,
        boolean open,
        Set<MealTime> meals,
        List<Slot> timings,
        List<Line> menu) {

    public record Slot(DayOfWeek day, LocalTime open, LocalTime close) {}

    public record Line(Dish dish, BigDecimal price, int prepMinutes) {}

    private static final Set<String> LOW_PRIORITY =
            Set.of("Beverages", "Desserts", "Sides", "Snacks", "Breads", "Soups", "Bakery", "Healthy");
    private static final Set<String> BREAKFAST_CATEGORIES = Set.of("Breakfast", "Tiffins");

    /**
     * One representative menu line per served meal (in breakfast, lunch, dinner order), each with a
     * different dish photo. Used to build the restaurant image: a breakfast-only place gets a breakfast
     * dish photo, a place serving all three gets a collage of a breakfast, a lunch and a dinner dish.
     */
    public List<Line> heroLines() {
        List<Line> heroes = new ArrayList<>();
        Set<String> usedWiki = new HashSet<>();
        for (MealTime meal : meals) {
            Line best = null;
            if (meal == MealTime.BREAKFAST) best = pick(meal, usedWiki, BREAKFAST_CATEGORIES, false);
            if (best == null) best = pick(meal, usedWiki, null, true);
            if (best == null) best = pick(meal, usedWiki, null, false);
            if (best != null) {
                heroes.add(best);
                usedWiki.add(best.dish().wiki());
            }
        }
        return heroes;
    }

    private Line pick(MealTime meal, Set<String> usedWiki, Set<String> onlyCategories, boolean mainsOnly) {
        Line best = null;
        for (Line l : menu) {
            Dish d = l.dish();
            if (!d.meals().contains(meal) || usedWiki.contains(d.wiki())) continue;
            if (onlyCategories != null && !onlyCategories.contains(d.category())) continue;
            if (mainsOnly && LOW_PRIORITY.contains(d.category())) continue;
            if (best == null || l.price().compareTo(best.price()) > 0) best = l;
        }
        return best;
    }
}
