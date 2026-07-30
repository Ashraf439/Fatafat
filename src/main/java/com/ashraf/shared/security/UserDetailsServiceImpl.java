package com.ashraf.shared.security;

import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.core.entity.Permissions;
import com.ashraf.core.entity.User;
import com.ashraf.core.repository.PermissionRepository;
import com.ashraf.core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    @Value("${user.not.found}")
    private String userNotFound;

    public UserDetailsServiceImpl(UserRepository userRepository, PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public CustomUserDetails loadUserByUsername(String username) {
        // your two repository calls + new CustomUserDetails(...) go here
        User user = userRepository.findByEmailWithRoles(username)
                .orElseThrow(() -> new UsernameNotFoundException(String.format(userNotFound,username)));

        List<Permissions> permissions =permissionRepository.getAllPermissionsByUserId(user.getId());

        return new CustomUserDetails(user,permissions);
    }
}
