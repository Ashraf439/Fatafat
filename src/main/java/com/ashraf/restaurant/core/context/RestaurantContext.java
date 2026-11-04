package com.ashraf.restaurant.core.context;

/**
 * Holds the restaurant the client has explicitly selected for the current request (an owner can
 * now own several restaurants, so "which one am I managing" has to come from somewhere). Backed by
 * the X-Restaurant-Id header via RestaurantContextFilter. When absent, callers fall back to the
 * user's default restaurant — see RestaurantAccessService.
 */
public final class RestaurantContext {
    private static final ThreadLocal<String> SELECTED = new ThreadLocal<>();

    private RestaurantContext() {
    }

    public static void set(String restaurantPublicId) {
        SELECTED.set(restaurantPublicId);
    }

    public static String get() {
        return SELECTED.get();
    }

    public static void clear() {
        SELECTED.remove();
    }
}