package com.ashraf.admin;

import com.ashraf.restaurant.entity.Restaurant;
import com.ashraf.rider.entity.Rider;
import com.ashraf.core.entity.User;
import com.ashraf.core.enums.Status;
import com.ashraf.shared.exception.AccountAlreadyActivatedException;
import com.ashraf.shared.exception.UserDoesNotExistException;
import com.ashraf.restaurant.repository.RestaurantRepository;
import com.ashraf.rider.repository.RiderRepository;
import com.ashraf.core.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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
    public void approveRestaurants(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new UserDoesNotExistException("Restaurant does not exist."));
        activateUser(restaurant.getOwnerUser());
    }

    @Transactional
    public void approveRiders(Long id) {
        Rider rider = riderRepository.findById(id)
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