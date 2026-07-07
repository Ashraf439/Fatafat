package com.ashraf.repository;

import com.ashraf.entity.AddressNormalized;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<AddressNormalized, Long> {
}
