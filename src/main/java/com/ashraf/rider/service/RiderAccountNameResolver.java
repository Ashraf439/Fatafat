package com.ashraf.rider.service;

import com.ashraf.auth.spi.AccountNameResolver;
import com.ashraf.core.entity.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RiderAccountNameResolver implements AccountNameResolver {
    @Override
    public Optional<String> resolveName(User user) {
        return user.getRider() != null
                ? Optional.ofNullable(user.getRider().getName())
                : Optional.empty();
    }
}