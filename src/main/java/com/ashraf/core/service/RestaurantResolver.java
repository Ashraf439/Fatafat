package com.ashraf.core.service;

import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class RestaurantResolver {
    private static final Pattern UUID_FORMAT = Pattern.compile("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$");
    private final RestaurantRepository restaurantRepository;

    public Optional<Restaurant> find(String ref) {
        if(ref == null || ref.isBlank()) {
            return Optional.empty();
        }
        if(UUID_FORMAT.matcher(ref).matches()) {
            return restaurantRepository.findByPublicId(UUID.fromString(ref));
        }
        return restaurantRepository.findBySlug(ref.toLowerCase(Locale.ROOT));
    }
}
