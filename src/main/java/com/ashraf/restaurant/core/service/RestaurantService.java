package com.ashraf.restaurant.core.service;

import com.ashraf.core.entity.User;
import com.ashraf.restaurant.core.dto.RestaurantSummaryResponse;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.shared.storage.ImageStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantAccessService restaurantAccessService;
    private final ImageStorageService imageStorageService;

    public RestaurantService(RestaurantRepository restaurantRepository,
                             RestaurantAccessService restaurantAccessService,
                             ImageStorageService imageStorageService) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantAccessService = restaurantAccessService;
        this.imageStorageService = imageStorageService;
    }

    @Transactional
    public String updateRestaurantImage(User user, MultipartFile file) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(user);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        String oldPublicId = restaurant.getImagePublicId();

        ImageStorageService.UploadedImage uploaded = imageStorageService.upload(file, "restaurants/" + restaurantId);
        restaurant.setImageUrl(uploaded.url());
        restaurant.setImagePublicId(uploaded.publicId());
        restaurantRepository.save(restaurant);

        if (oldPublicId != null) {
            imageStorageService.delete(oldPublicId);
        }

        return uploaded.url();
    }

    public boolean getStatus(User user) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(user);
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"))
                .getIsOpen();
    }

    @Transactional
    public boolean toggleStatus(User user) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(user);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        restaurant.setIsOpen(!restaurant.getIsOpen());
        restaurantRepository.save(restaurant);
        return restaurant.getIsOpen();
    }

    @Transactional
    public List<RestaurantSummaryResponse> getRestaurantsForCustomer() {
        List<Restaurant> restaurant = restaurantRepository.findAll();

        return  restaurant.stream().map(r -> new RestaurantSummaryResponse(r.getId(),r.getName(), r.getIsOpen(),r.getAddresses().isEmpty() ? null : r.getAddresses().getFirst().getCity(), r.getImageUrl())).toList();
    }
}