package com.ashraf.core.repository;

import com.ashraf.core.entity.UserRoleId;
import com.ashraf.core.entity.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRolesRepository extends JpaRepository<UserRoles, UserRoleId> {
    //boolean existsByUserRoles_Role_Name(String roleName);
}
