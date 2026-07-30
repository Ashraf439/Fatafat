package com.ashraf.core.repository;

import com.ashraf.core.entity.RolePermissionId;
import com.ashraf.core.entity.RolePermissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionsRepository extends JpaRepository<RolePermissions, RolePermissionId>  {
    boolean existsById(RolePermissionId id);
}
