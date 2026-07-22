package com.ashraf.security;

import com.ashraf.entity.Permissions;
import com.ashraf.entity.User;
import com.ashraf.repository.PermissionRepository;
import com.ashraf.repository.UserRepository;
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
