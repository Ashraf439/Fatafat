package com.ashraf.repository;

import com.ashraf.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolesRepository extends JpaRepository<Role,Long> {
}
