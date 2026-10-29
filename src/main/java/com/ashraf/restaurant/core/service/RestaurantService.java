package com.ashraf.restaurant.core.service;

import com.ashraf.core.entity.User;
import com.ashraf.restaurant.core.dto.RestaurantProfileResponse;
import com.ashraf.restaurant.core.dto.RestaurantProfileUpdateRequest;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.entity.RestaurantAddress;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.shared.storage.ImageStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


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

    @Transactional(readOnly = true)
    public RestaurantProfileResponse getMyProfile(User user) {
        Restaurant restaurant = restaurantAccessService.getRestaurantForUser(user);
        return toProfileResponse(restaurant);
    }

    @Transactional
    public RestaurantProfileResponse updateProfile(User user, RestaurantProfileUpdateRequest req) {
        Restaurant restaurant = restaurantAccessService.getRestaurantForUser(user);

        if (req.getName() != null && !req.getName().isBlank()) {
            restaurant.setName(req.getName());
        }

        RestaurantAddress address = restaurant.getAddresses().stream().findFirst().orElseGet(() -> {
            RestaurantAddress a = new RestaurantAddress();
            a.setIsDefault(true);
            a.setCountry("India");
            a.setRestaurant(restaurant);
            restaurant.getAddresses().add(a);
            return a;
        });

        if (req.getStreet() != null) address.setStreet(req.getStreet());
        if (req.getLandmark() != null) address.setLandmark(req.getLandmark());
        if (req.getCity() != null) address.setCity(req.getCity());
        if (req.getState() != null) address.setState(req.getState());
        if (req.getPincode() != null) address.setPincode(req.getPincode());
        if (req.getLatitude() != null) address.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) address.setLongitude(req.getLongitude());

        restaurantRepository.save(restaurant);
        return toProfileResponse(restaurant);
    }

    private RestaurantProfileResponse toProfileResponse(Restaurant restaurant) {
        RestaurantAddress address = restaurant.getAddresses().stream().findFirst().orElse(null);
        return new RestaurantProfileResponse(
                restaurant.getPublicId(),
                restaurant.getName(),
                restaurant.getOwnerName(),
                restaurant.getRestaurantOnboardingStatus(),
                restaurant.getIsOpen(),
                restaurant.getImageUrl(),
                address != null ? address.getStreet() : null,
                address != null ? address.getLandmark() : null,
                address != null ? address.getCity() : null,
                address != null ? address.getState() : null,
                address != null ? address.getPincode() : null,
                address != null ? address.getLatitude() : null,
                address != null ? address.getLongitude() : null,
                restaurant.getFssaiLicense(),
                restaurant.getGstin()
        );
    }

    @Transactional
    public String updateRestaurantImage(User user, MultipartFile file) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(user);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        String oldPublicId = restaurant.getImagePublicId();

        ImageStorageService.UploadedImage uploaded =
                imageStorageService.upload(file, "restaurants/" + restaurantId);
        restaurant.setImageUrl(uploaded.url());
        restaurant.setImagePublicId(uploaded.publicId());
        restaurantRepository.save(restaurant);

        if (oldPublicId != null) {
            imageStorageService.delete(oldPublicId);
        }

        return uploaded.url();
    }

    @Transactional(readOnly = true)
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
}