package com.ashraf.auth.spi;

import com.ashraf.core.entity.User;

import java.util.Optional;

public interface AccountNameResolver {
    Optional<String> resolveName(User user);
}