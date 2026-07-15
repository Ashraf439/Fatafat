package com.ashraf.repository;

import com.ashraf.entity.UserRoleId;
import com.ashraf.entity.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRolesRepository extends JpaRepository<UserRoles, UserRoleId> {
}
