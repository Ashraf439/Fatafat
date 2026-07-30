package com.ashraf.customer.security;

import com.ashraf.core.entity.Permissions;
import com.ashraf.core.entity.User;
import com.ashraf.core.enums.Status;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.stream.Collectors;

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
        return user.getUserRoles().stream()
                .map(ur -> new SimpleGrantedAuthority("ROLE_" + ur.getRole().getName()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() != Status.SUSPENDED;
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