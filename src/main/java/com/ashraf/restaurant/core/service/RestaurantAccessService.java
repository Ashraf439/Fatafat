package com.ashraf.restaurant.core.service;

import com.ashraf.core.entity.User;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.restaurant.staff.entity.RestaurantStaff;
import com.ashraf.restaurant.staff.enums.StaffStatus;
import com.ashraf.restaurant.staff.repository.RestaurantStaffRepository;
import com.ashraf.shared.exception.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class RestaurantAccessService {
    private final RestaurantRepository restaurantRepository;
    private final RestaurantStaffRepository restaurantStaffRepository;

    public RestaurantAccessService(RestaurantRepository restaurantRepository,RestaurantStaffRepository restaurantStaffRepository) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantStaffRepository = restaurantStaffRepository;
    }

    public Long resolveRestaurantId(User user){
        if(restaurantRepository.existsByOwnerUser_Id(user.getId())){
            return restaurantRepository.findByOwnerUser_Id(user.getId()).orElseThrow(() -> new AccessDeniedException("Owner restaurant not found"))
                    .getId();
        }
        RestaurantStaff staff = restaurantStaffRepository.findByUser_IdAndStatus(user.getId(), StaffStatus.ACTIVE).orElse(null);

        if(staff != null) {
            return  staff.getRestaurant().getId();
        }
        throw new AccessDeniedException("User does not belong to any restaurant");
    }

    public void assertBelongs(User user, Long restaurantId){
        boolean isOwner = restaurantRepository.existsByIdAndOwnerUser_Id(restaurantId, user.getId());
        boolean isStaff = restaurantStaffRepository.existsByRestaurant_IdAndUser_IdAndStatus(restaurantId, user.getId(), StaffStatus.ACTIVE);
        if(!isOwner && !isStaff) {
            throw new AccessDeniedException("No access");
        }
    }
}
