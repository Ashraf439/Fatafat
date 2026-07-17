package com.ashraf.repository;

import com.ashraf.entity.RolePermissionId;
import com.ashraf.entity.RolePermissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionsRepository extends JpaRepository<RolePermissions, RolePermissionId>  {
    boolean existsById(RolePermissionId id);
}
