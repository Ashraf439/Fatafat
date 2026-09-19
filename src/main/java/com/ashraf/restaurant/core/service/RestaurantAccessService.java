package com.ashraf.restaurant.core.service;

import com.ashraf.core.entity.User;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.restaurant.staff.entity.RestaurantStaff;
import com.ashraf.restaurant.staff.enums.StaffStatus;
import com.ashraf.restaurant.staff.repository.RestaurantStaffRepository;
import com.ashraf.shared.exception.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RestaurantAccessService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantStaffRepository restaurantStaffRepository;

    public RestaurantAccessService(RestaurantRepository restaurantRepository,
                                   RestaurantStaffRepository restaurantStaffRepository) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantStaffRepository = restaurantStaffRepository;
    }

    /** Restaurant the user owns, or works at as ACTIVE staff. Empty if neither. */
    @Transactional(readOnly = true)
    public Optional<Restaurant> findRestaurantForUser(User user) {
        Optional<Restaurant> owned = restaurantRepository.findByOwnerUser_Id(user.getId());
        if (owned.isPresent()) {
            return owned;
        }
        return restaurantStaffRepository
                .findByUser_IdAndStatus(user.getId(), StaffStatus.ACTIVE)
                .map(RestaurantStaff::getRestaurant);
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