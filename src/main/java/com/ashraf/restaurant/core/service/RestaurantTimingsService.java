package com.ashraf.restaurant.core.service;

import com.ashraf.core.entity.User;
import com.ashraf.restaurant.core.dto.TimingSlotRequest;
import com.ashraf.restaurant.core.dto.TimingSlotResponse;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.entity.RestaurantTimings;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.restaurant.core.repository.RestaurantTimingsRepository;
import com.ashraf.restaurant.core.util.TimingSlotValidator;
import com.ashraf.shared.exception.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantTimingsService {

    private final RestaurantTimingsRepository restaurantTimingsRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantAccessService restaurantAccessService;

    public RestaurantTimingsService(RestaurantTimingsRepository restaurantTimingsRepository,
                                    RestaurantRepository restaurantRepository,
                                    RestaurantAccessService restaurantAccessService) {
        this.restaurantTimingsRepository = restaurantTimingsRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantAccessService = restaurantAccessService;
    }

    public List<TimingSlotResponse> getTimings(User user) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(user);
        return restaurantTimingsRepository.findByRestaurant_IdOrderByDayOfWeekAscOpenTimeAsc(restaurantId)
                .stream()
                .map(TimingSlotResponse::new)
                .toList();
    }

    public List<TimingSlotResponse> getTimingsForCustomer(Long restaurantId) {
        return restaurantTimingsRepository.findByRestaurant_IdOrderByDayOfWeekAscOpenTimeAsc(restaurantId)
                .stream()
                .map(TimingSlotResponse::new)
                .toList();
    }

    /**
     * Fully replaces this restaurant's weekly schedule with the given shifts.
     * Days omitted from the list are treated as closed.
     */
    @Transactional
    public List<TimingSlotResponse> replaceTimings(User user, List<TimingSlotRequest> slots) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(user);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with ID: " + restaurantId));

        TimingSlotValidator.validate(slots);

        restaurantTimingsRepository.deleteByRestaurant_Id(restaurantId);
        restaurantTimingsRepository.flush();

        List<RestaurantTimings> entities = new ArrayList<>();
        for (TimingSlotRequest slot : slots) {
            entities.add(toEntity(slot, restaurant));
        }

        return restaurantTimingsRepository.saveAll(entities)
                .stream()
                .map(TimingSlotResponse::new)
                .toList();
    }

    /**
     * Adds a single shift without touching the rest of the week's schedule.
     */
    @Transactional
    public TimingSlotResponse addTiming(User user, TimingSlotRequest slot) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(user);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with ID: " + restaurantId));

        List<TimingSlotRequest> proposed = currentSlotsAsRequests(restaurantId);
        proposed.add(slot);
        TimingSlotValidator.validate(proposed);

        RestaurantTimings saved = restaurantTimingsRepository.save(toEntity(slot, restaurant));
        return new TimingSlotResponse(saved);
    }

    /**
     * Edits a single shift by its own id, validated against the rest of that day's shifts.
     */
    @Transactional
    public TimingSlotResponse updateTiming(User user, Long timingId, TimingSlotRequest slot) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(user);
        RestaurantTimings timing = restaurantTimingsRepository.findById(timingId)
                .orElseThrow(() -> new IllegalArgumentException("Timing slot not found with ID: " + timingId));

        if (!timing.getRestaurant().getId().equals(restaurantId)) {
            throw new AccessDeniedException("Timing slot does not belong to your restaurant");
        }

        // Validate this shift's new values against every other existing shift (excluding itself).
        List<RestaurantTimings> others = restaurantTimingsRepository.findByRestaurant_IdOrderByDayOfWeekAscOpenTimeAsc(restaurantId)
                .stream()
                .filter(t -> !t.getId().equals(timingId))
                .toList();
        List<TimingSlotRequest> proposed = others.stream().map(this::toRequest).collect(Collectors.toCollection(ArrayList::new));
        proposed.add(slot);
        TimingSlotValidator.validate(proposed);

        timing.setDayOfWeek(slot.getDayOfWeek());
        timing.setOpenTime(slot.getOpenTime());
        timing.setCloseTime(slot.getCloseTime());

        return new TimingSlotResponse(restaurantTimingsRepository.save(timing));
    }

    /**
     * Removes a single shift.
     */
    @Transactional
    public void deleteTiming(User user, Long timingId) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(user);
        RestaurantTimings timing = restaurantTimingsRepository.findById(timingId)
                .orElseThrow(() -> new IllegalArgumentException("Timing slot not found with ID: " + timingId));

        if (!timing.getRestaurant().getId().equals(restaurantId)) {
            throw new AccessDeniedException("Timing slot does not belong to your restaurant");
        }

        restaurantTimingsRepository.delete(timing);
    }

    private List<TimingSlotRequest> currentSlotsAsRequests(Long restaurantId) {
        return restaurantTimingsRepository.findByRestaurant_IdOrderByDayOfWeekAscOpenTimeAsc(restaurantId)
                .stream()
                .map(this::toRequest)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private TimingSlotRequest toRequest(RestaurantTimings timing) {
        TimingSlotRequest request = new TimingSlotRequest();
        request.setDayOfWeek(timing.getDayOfWeek());
        request.setOpenTime(timing.getOpenTime());
        request.setCloseTime(timing.getCloseTime());
        return request;
    }

    private RestaurantTimings toEntity(TimingSlotRequest slot, Restaurant restaurant) {
        RestaurantTimings timing = new RestaurantTimings();
        timing.setRestaurant(restaurant);
        timing.setDayOfWeek(slot.getDayOfWeek());
        timing.setOpenTime(slot.getOpenTime());
        timing.setCloseTime(slot.getCloseTime());
        return timing;
    }
}