package com.ashraf.repository;

import com.ashraf.entity.RolePermissionId;
import com.ashraf.entity.RolePermissions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionsRepository extends JpaRepository<RolePermissions, RolePermissionId> {
}
