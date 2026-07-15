package com.ashraf.repository;

import com.ashraf.entity.Permissions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionsRepository extends JpaRepository<Permissions, Long> {
}
