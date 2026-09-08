package com.ashraf.restaurant.core.service;

import com.ashraf.core.entity.User;
import com.ashraf.restaurant.core.dto.CustomerMenuResponse;
import com.ashraf.restaurant.core.dto.MenuCsvRow;
import com.ashraf.restaurant.core.dto.MenuResponse;
import com.ashraf.restaurant.core.entity.Menu;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.enums.FoodType;
import com.ashraf.restaurant.core.repository.MenuRepository;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantAccessService restaurantAccessService;

    public MenuService(MenuRepository menuRepository,
                       RestaurantRepository restaurantRepository,
                       RestaurantAccessService restaurantAccessService) {
        this.menuRepository = menuRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantAccessService = restaurantAccessService;
    }

    @Transactional
    public List<Menu> saveMenu(MultipartFile file, User owner) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(owner);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with ID: " + restaurantId));

        List<MenuCsvRow> rows;
        try (Reader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CsvToBean<MenuCsvRow> csvToBean = new CsvToBeanBuilder<MenuCsvRow>(reader)
                    .withType(MenuCsvRow.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            rows = csvToBean.parse();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV file contained no menu rows.");
        }

        List<String> errors = new ArrayList<>();
        List<Menu> menuItems = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2; // +1 for 0-index, +1 for the header row
            MenuCsvRow row = rows.get(i);
            String rowErrors = validateRow(row, rowNum);
            if (rowErrors != null) {
                errors.add(rowErrors);
                continue;
            }
            menuItems.add(toMenu(row, restaurant));
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("CSV validation failed: " + String.join("; ", errors));
        }

        return menuRepository.saveAll(menuItems);
    }

    private String validateRow(MenuCsvRow row, int rowNum) {
        List<String> issues = new ArrayList<>();

        if (row.getDishName() == null || row.getDishName().isBlank()) {
            issues.add("DishName is required");
        }
        if (row.getPrice() == null || row.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            issues.add("Price must be a positive number");
        }
        if (row.getFoodType() == null || parseFoodType(row.getFoodType()) == null) {
            issues.add("FoodType must be one of " + java.util.Arrays.toString(FoodType.values()));
        }

        return issues.isEmpty() ? null : "row " + rowNum + ": " + String.join(", ", issues);
    }

    private FoodType parseFoodType(String value) {
        try {
            return FoodType.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private Menu toMenu(MenuCsvRow row, Restaurant restaurant) {
        Menu menu = new Menu();
        menu.setRestaurant(restaurant);
        menu.setDishName(row.getDishName().trim());
        menu.setDescription(row.getDescription());
        menu.setPrice(row.getPrice());
        menu.setFoodType(parseFoodType(row.getFoodType()));
        menu.setCategory(row.getCategory());
        menu.setPreparationTimeMinutes(row.getPreparationTimeMinutes());
        return menu;
    }
    public List<MenuResponse> getMenu(User user) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(user);
        return menuRepository.findByRestaurant_Id(restaurantId)
                .stream()
                .map(MenuResponse::new)
                .toList();
    }
    public CustomerMenuResponse getMenuForCustomer(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
        List<Menu> menuItems = menuRepository.findByRestaurant_Id(restaurantId);
        List<MenuResponse> menuResponses = menuItems.stream().map(MenuResponse::new).toList();
        Map<String, List<MenuResponse>> grouped = menuResponses.stream().collect(Collectors.groupingBy(MenuResponse::getCategory));
        return new CustomerMenuResponse(restaurantId, restaurant.getName(), restaurant.getIsOpen(), grouped);
    }

}