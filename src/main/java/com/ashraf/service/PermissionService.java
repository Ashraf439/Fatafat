package com.ashraf.service;

import com.ashraf.entity.UserPermissions;
import com.ashraf.enums.PermissionEffect;
import com.ashraf.repository.PermissionRepository;
import com.ashraf.repository.UserPermissionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final UserPermissionsRepository userPermissionsRepository;

    public boolean hasPermission(Long userId, String permissionName) {
        Optional<UserPermissions> directPermissionOpt = userPermissionsRepository.findByUser_IdAndPermission_Name(userId,permissionName);
        if(directPermissionOpt.isPresent()) {
            PermissionEffect permissionEffect = directPermissionOpt.get().getEffect();
            return permissionEffect == PermissionEffect.GRANT;
        }
        return permissionRepository.userHasPermission(userId,permissionName);
    }
}