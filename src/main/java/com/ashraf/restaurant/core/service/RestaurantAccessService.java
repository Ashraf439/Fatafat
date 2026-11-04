package com.ashraf.restaurant.core.service;

import com.ashraf.core.entity.User;
import com.ashraf.restaurant.core.context.RestaurantContext;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.restaurant.staff.entity.RestaurantStaff;
import com.ashraf.restaurant.staff.enums.StaffStatus;
import com.ashraf.restaurant.staff.repository.RestaurantStaffRepository;
import com.ashraf.shared.exception.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RestaurantAccessService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantStaffRepository restaurantStaffRepository;

    public RestaurantAccessService(RestaurantRepository restaurantRepository,
                                   RestaurantStaffRepository restaurantStaffRepository) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantStaffRepository = restaurantStaffRepository;
    }

    /**
     * Restaurant the user is currently managing. An owner can have several restaurants, so this
     * honors the X-Restaurant-Id header (via RestaurantContext) when it names one the user actually
     * has access to; otherwise it falls back to their first owned restaurant, or the one they work
     * at as ACTIVE staff. Empty only if the user has no restaurant at all.
     */
    @Transactional(readOnly = true)
    public Optional<Restaurant> findRestaurantForUser(User user) {
        String selected = RestaurantContext.get();
        if (selected != null) {
            Optional<Restaurant> bySelection = resolveSelected(user, selected);
            if (bySelection.isPresent()) {
                return bySelection;
            }
            // Header named a restaurant that doesn't exist or isn't this user's — ignore it
            // and fall through to their default, rather than failing the whole request.
        }

        List<Restaurant> owned = restaurantRepository.findAllByOwnerUser_IdOrderByIdAsc(user.getId());
        if (!owned.isEmpty()) {
            return Optional.of(owned.get(0));
        }
        return restaurantStaffRepository
                .findByUser_IdAndStatus(user.getId(), StaffStatus.ACTIVE)
                .map(RestaurantStaff::getRestaurant);
    }

    private Optional<Restaurant> resolveSelected(User user, String selectedPublicId) {
        UUID publicId;
        try {
            publicId = UUID.fromString(selectedPublicId);
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
        return restaurantRepository.findByPublicId(publicId)
                .filter(r -> isOwner(user, r.getId()) || isActiveStaff(user, r.getId()));
    }

    /** Every restaurant this user can manage — their own restaurants, or the one they staff. For the restaurant switcher. */
    @Transactional(readOnly = true)
    public List<Restaurant> listRestaurantsForUser(User user) {
        List<Restaurant> owned = restaurantRepository.findAllByOwnerUser_IdOrderByIdAsc(user.getId());
        if (!owned.isEmpty()) {
            return owned;
        }
        return restaurantStaffRepository
                .findByUser_IdAndStatus(user.getId(), StaffStatus.ACTIVE)
                .map(RestaurantStaff::getRestaurant)
                .map(List::of)
                .orElse(List.of());
    }

    /** Same as above, but throws if the user has no restaurant. */
    @Transactional(readOnly = true)
    public Restaurant getRestaurantForUser(User user) {
        return findRestaurantForUser(user)
                .orElseThrow(() -> new AccessDeniedException("User does not belong to any restaurant"));
    }

    @Transactional(readOnly = true)
    public Long resolveRestaurantId(User user) {
        return getRestaurantForUser(user).getId();
    }

    @Transactional(readOnly = true)
    public boolean isOwner(User user, Long restaurantId) {
        return restaurantRepository.existsByIdAndOwnerUser_Id(restaurantId, user.getId());
    }

    @Transactional(readOnly = true)
    public boolean isActiveStaff(User user, Long restaurantId) {
        return restaurantStaffRepository.existsByRestaurant_IdAndUser_IdAndStatus(
                restaurantId, user.getId(), StaffStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public void assertBelongs(User user, Long restaurantId) {
        if (!isOwner(user, restaurantId) && !isActiveStaff(user, restaurantId)) {
            throw new AccessDeniedException("No access");
        }
    }
}