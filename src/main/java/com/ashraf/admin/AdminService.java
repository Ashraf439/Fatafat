package com.ashraf.admin;

import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.rider.entity.Rider;
import com.ashraf.core.entity.User;
import com.ashraf.core.enums.Status;
import com.ashraf.shared.exception.AccountAlreadyActivatedException;
import com.ashraf.shared.exception.UserDoesNotExistException;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.rider.repository.RiderRepository;
import com.ashraf.core.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final RiderRepository riderRepository;

    public AdminService(UserRepository userRepository, RestaurantRepository restaurantRepository, RiderRepository riderRepository) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.riderRepository = riderRepository;
    }

    @Transactional
    public void approveRestaurants(UUID id) {
        Restaurant restaurant = restaurantRepository.findByPublicId(id)
                .orElseThrow(() -> new UserDoesNotExistException("Restaurant does not exist."));
        activateUser(restaurant.getOwnerUser());
    }

    @Transactional
    public void approveRiders(UUID id) {
        Rider rider = riderRepository.findByPublicId(id)
                .orElseThrow(() -> new UserDoesNotExistException("Rider does not exist."));
        activateUser(rider.getUser());
    }

    private void activateUser(User user) {
        if (user.getStatus() == Status.ACTIVE) {
            throw new AccountAlreadyActivatedException("Account already activated.");
        }
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
    }
}