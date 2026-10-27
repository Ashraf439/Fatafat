package com.ashraf.customer.service;

import com.ashraf.core.entity.User;
import com.ashraf.customer.dto.WishlistItemResponse;
import com.ashraf.customer.entity.Customer;
import com.ashraf.customer.entity.Wishlist;
import com.ashraf.customer.repository.WishlistRepository;
import com.ashraf.restaurant.core.entity.Menu;
import com.ashraf.restaurant.core.repository.MenuRepository;
import com.ashraf.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WishlistService {
    private final WishlistRepository wishlistRepository;
    private final MenuRepository menuRepository;
    private final CustomerLookupService customerLookup;

    public WishlistService(WishlistRepository wishlistRepository, MenuRepository menuRepository,
                           CustomerLookupService customerLookup) {
        this.wishlistRepository = wishlistRepository;
        this.menuRepository = menuRepository;
        this.customerLookup = customerLookup;
    }

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> list(User user) {
        Customer customer = customerLookup.requireCustomer(user);
        return wishlistRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                .stream().map(WishlistItemResponse::from).toList();
    }

    @Transactional
    public void add(User user, UUID menuId) {
        Customer customer = customerLookup.requireCustomer(user);
        if (wishlistRepository.existsByCustomer_IdAndMenu_PublicId(customer.getId(), menuId)) return;
        Menu menu = menuRepository.findByPublicId(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("Dish not found"));
        Wishlist w = new Wishlist();
        w.setCustomer(customer);
        w.setMenu(menu);
        wishlistRepository.save(w);
    }

    @Transactional
    public void remove(User user, UUID menuId) {
        Customer customer = customerLookup.requireCustomer(user);
        wishlistRepository.findByCustomer_IdAndMenu_PublicId(customer.getId(), menuId)
                .ifPresent(wishlistRepository::delete);
    }
}