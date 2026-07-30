package com.ashraf.core.service;

import com.ashraf.core.entity.UserPermissions;
import com.ashraf.core.enums.PermissionEffect;
import com.ashraf.core.repository.PermissionRepository;
import com.ashraf.core.repository.UserPermissionsRepository;
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