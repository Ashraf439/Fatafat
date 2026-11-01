package com.ashraf.customer.service;

import com.ashraf.core.entity.User;
import com.ashraf.customer.dto.CustomerProfileResponse;
import com.ashraf.customer.dto.CustomerProfileUpdateRequest;
import com.ashraf.customer.entity.Customer;
import com.ashraf.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerProfileService {

    private final CustomerLookupService customerLookupService;
    private final CustomerRepository customerRepository;

    public CustomerProfileService(CustomerLookupService customerLookupService, CustomerRepository customerRepository) {
        this.customerLookupService = customerLookupService;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getMyProfile(User user) {
        Customer customer = customerLookupService.requireCustomer(user);
        return toResponse(customer);
    }

    @Transactional
    public CustomerProfileResponse updateProfile(User user, CustomerProfileUpdateRequest req) {
        Customer customer = customerLookupService.requireCustomer(user);

        if (req.getName() != null && !req.getName().isBlank()) {
            customer.setName(req.getName());
        }
        if (req.getPhoneNumber() != null && !req.getPhoneNumber().isBlank()) {
            customer.setPhoneNumber(req.getPhoneNumber());
        }
        if (req.getDateOfBirth() != null) {
            customer.setDateOfBirth(req.getDateOfBirth());
        }

        customerRepository.save(customer);
        return toResponse(customer);
    }

    private CustomerProfileResponse toResponse(Customer customer) {
        return new CustomerProfileResponse(
                customer.getId(),
                customer.getName(),
                customer.getUser().getEmail(),
                customer.getPhoneNumber() != null ? customer.getPhoneNumber() : customer.getUser().getPhoneNumber(),
                customer.getDateOfBirth(),
                customer.getLoyaltyPoints(),
                customer.getWalletBalance()
        );
    }
}