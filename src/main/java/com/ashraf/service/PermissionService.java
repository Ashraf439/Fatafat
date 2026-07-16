package com.ashraf.service;

import com.ashraf.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public boolean hasPermission(Long userId, String permissionName) {
        return permissionRepository.userHasPermission(userId, permissionName);
    }
}