package com.ashraf.customer.service;

import com.ashraf.auth.spi.AccountNameResolver;
import com.ashraf.core.entity.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomerAccountNameResolver implements AccountNameResolver {
    @Override
    public Optional<String> resolveName(User user) {
        return user.getCustomer() != null
                ? Optional.ofNullable(user.getCustomer().getName())
                : Optional.empty();
    }
}