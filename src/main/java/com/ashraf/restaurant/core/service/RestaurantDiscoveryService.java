package com.ashraf.restaurant.core.service;

import com.ashraf.restaurant.core.dto.RestaurantDetailResponse;
import com.ashraf.restaurant.core.dto.RestaurantSummaryResponse;
import com.ashraf.restaurant.core.dto.TimingSlotResponse;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.entity.RestaurantAddress;
import com.ashraf.restaurant.core.entity.RestaurantTimings;
import com.ashraf.restaurant.core.repository.MenuRepository;
import com.ashraf.restaurant.core.repository.RestaurantAddressRepository;
import com.ashraf.restaurant.core.repository.RestaurantMenuStats;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import com.ashraf.shared.dto.PageResponse;
import com.ashraf.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only, customer-facing restaurant discovery: search + pagination for the listing page and
 * a detail view for the restaurant page. Extras (addresses, menu stats) are loaded in one batch
 * query per page rather than per restaurant.
 */
@Service
public class RestaurantDiscoveryService {

    static final int DEFAULT_PAGE_SIZE = 12;
    static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_QUERY_LENGTH = 100;

    private final RestaurantRepository restaurantRepository;
    private final RestaurantAddressRepository restaurantAddressRepository;
    private final MenuRepository menuRepository;

    public RestaurantDiscoveryService(RestaurantRepository restaurantRepository,
                                      RestaurantAddressRepository restaurantAddressRepository,
                                      MenuRepository menuRepository) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantAddressRepository = restaurantAddressRepository;
        this.menuRepository = menuRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<RestaurantSummaryResponse> search(String query, String city,
                                                          boolean openOnly, boolean pureVeg,
                                                          int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        Page<Restaurant> result = restaurantRepository.searchLive(
                normalize(query), normalize(city), openOnly, pureVeg,
                PageRequest.of(safePage, safeSize));

        List<Long> ids = result.getContent().stream().map(Restaurant::getId).toList();
        Map<Long, RestaurantAddress> addresses = addressesByRestaurant(ids);
        Map<Long, RestaurantMenuStats> stats = statsByRestaurant(ids);

        List<RestaurantSummaryResponse> items = result.getContent().stream()
                .map(r -> toSummary(r, addresses.get(r.getId()), stats.get(r.getId())))
                .toList();
        return PageResponse.of(result, items);
    }

    @Transactional(readOnly = true)
    public List<String> listCities() {
        return restaurantAddressRepository.findLiveCities();
    }

    @Transactional(readOnly = true)
    public RestaurantDetailResponse getDetail(Long restaurantId) {
        Restaurant r = restaurantRepository.findById(restaurantId)
                .filter(x -> x.getRestaurantOnboardingStatus() == RestaurantOnboardingStatus.LIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

        RestaurantAddress address = addressesByRestaurant(List.of(restaurantId)).get(restaurantId);
        RestaurantMenuStats stats = statsByRestaurant(List.of(restaurantId)).get(restaurantId);

        List<TimingSlotResponse> timings = r.getTimings().stream()
                .sorted(Comparator.comparing(RestaurantTimings::getDayOfWeek)
                        .thenComparing(RestaurantTimings::getOpenTime))
                .map(TimingSlotResponse::new)
                .toList();

        return new RestaurantDetailResponse(
                r.getId(), r.getName(), r.getIsOpen(), r.getImageUrl(),
                address != null ? address.getStreet() : null,
                address != null ? address.getLandmark() : null,
                address != null ? address.getCity() : null,
                address != null ? address.getState() : null,
                address != null ? address.getPincode() : null,
                timings,
                itemCount(stats), startingPrice(stats), avgPrep(stats), pureVeg(stats));
    }

    // ---- helpers -------------------------------------------------------------------------

    private RestaurantSummaryResponse toSummary(Restaurant r, RestaurantAddress address,
                                                RestaurantMenuStats stats) {
        return new RestaurantSummaryResponse(
                r.getId(), r.getName(), r.getIsOpen(),
                address != null ? address.getStreet() : null,
                address != null ? address.getCity() : null,
                r.getImageUrl(),
                itemCount(stats), startingPrice(stats), avgPrep(stats), pureVeg(stats));
    }

    private Map<Long, RestaurantAddress> addressesByRestaurant(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, RestaurantAddress> picked = new HashMap<>();
        for (RestaurantAddress a : restaurantAddressRepository.findByRestaurant_IdIn(ids)) {
            Long rid = a.getRestaurant().getId();
            RestaurantAddress current = picked.get(rid);
            boolean better = current == null
                    || (!Boolean.TRUE.equals(current.getIsDefault()) && Boolean.TRUE.equals(a.getIsDefault()));
            if (better) {
                picked.put(rid, a);
            }
        }
        return picked;
    }

    private Map<Long, RestaurantMenuStats> statsByRestaurant(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return menuRepository.summarizeByRestaurantIds(ids).stream()
                .collect(Collectors.toMap(RestaurantMenuStats::getRestaurantId, Function.identity()));
    }

    private static Integer itemCount(RestaurantMenuStats s) {
        return s == null || s.getItemCount() == null ? 0 : s.getItemCount().intValue();
    }

    private static java.math.BigDecimal startingPrice(RestaurantMenuStats s) {
        return s == null ? null : s.getMinPrice();
    }

    private static Integer avgPrep(RestaurantMenuStats s) {
        return s == null || s.getAvgPrepMinutes() == null ? null : (int) Math.round(s.getAvgPrepMinutes());
    }

    private static Boolean pureVeg(RestaurantMenuStats s) {
        return s != null && s.getItemCount() != null && s.getItemCount() > 0
                && s.getItemCount().equals(s.getVegCount());
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        return v.length() > MAX_QUERY_LENGTH ? v.substring(0, MAX_QUERY_LENGTH) : v;
    }
}
