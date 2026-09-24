package com.ashraf.customer.service;

import com.ashraf.core.entity.User;
import com.ashraf.customer.entity.Customer;
import com.ashraf.customer.repository.CustomerRepository;
import com.ashraf.shared.exception.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the Customer profile behind an authenticated User. */
@Service
public class CustomerLookupService {

    private final CustomerRepository customerRepository;

    public CustomerLookupService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Customer requireCustomer(User user) {
        return customerRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new AccessDeniedException("No customer profile for this account"));
    }
}
