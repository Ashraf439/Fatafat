package com.ashraf.service.impl;

import com.ashraf.entity.User;
import com.ashraf.enums.Status;
import com.ashraf.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    @Value("${user.not.found}")
    private String userNotFound;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(String.format(userNotFound,email)));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(
                         user.getUserRoles().stream()
                                .map(ur -> new SimpleGrantedAuthority("ROLE_" + ur.getRole().getName()))
                                .toList()
                )
                .accountLocked(user.getStatus() == Status.SUSPENDED)
                .build();
    }
}
