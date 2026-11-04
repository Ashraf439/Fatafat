package com.ashraf.admin;

import com.ashraf.auth.spi.AccountNameResolver;
import com.ashraf.core.entity.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Admins have no sub-entity — their display name lives directly on User.name (set manually). */
@Component
public class AdminAccountNameResolver implements AccountNameResolver {
    @Override
    public Optional<String> resolveName(User user) {
        return Optional.ofNullable(user.getName()).filter(name -> !name.isBlank());
    }
}