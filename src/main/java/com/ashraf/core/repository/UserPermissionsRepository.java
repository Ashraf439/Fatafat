package com.ashraf.core.repository;

import com.ashraf.core.entity.UserPermissionId;
import com.ashraf.core.entity.UserPermissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPermissionsRepository extends JpaRepository<UserPermissions, UserPermissionId> {
    Optional<UserPermissions> findByUser_IdAndPermission_Name(Long userId, String permissionName);
}