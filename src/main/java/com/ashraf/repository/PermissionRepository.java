package com.ashraf.repository;

import com.ashraf.entity.Permissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permissions, Long> {

    @Query("""
        SELECT COUNT(p) > 0
        FROM UserRoles ur
        JOIN ur.role r
        JOIN RolePermissions rp ON rp.role.id = r.id
        JOIN rp.permission p
        WHERE ur.user.id = :userId
        AND p.name = :permissionName
        """)
    boolean userHasPermission(@Param("userId") Long userId, @Param("permissionName") String permissionName);
    boolean existsByName(String permissionName);
    Optional<Permissions> findByName(String permissionName);
}