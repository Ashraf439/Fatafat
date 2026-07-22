package com.ashraf.security;

import com.ashraf.entity.Permissions;
import com.ashraf.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomUserDetails implements UserDetails {

    private final User user;
    private final List<Permissions> permissions;

    public CustomUserDetails(User user, List<Permissions> permissions) {
        this.user = user;
        this.permissions = permissions;
    }



    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        Stream<String> roleAuthorities = user.getUserRoles().stream()
                .map(ur -> "ROLE_" + ur.getRole().getName());

        Stream<String> permissionAuthorities = permissions.stream()
                .map(Permissions::getName);

        return Stream.concat(roleAuthorities, permissionAuthorities)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEnabled() {
        return true; // suspended-check happens later in AuthService, not here
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // not implementing lockout for now
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // not implementing account expiry for now
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // not implementing credential expiry for now
    }

    public User getUser() {
        return user; // gives AuthService a way to get the full User back after auth
    }
}