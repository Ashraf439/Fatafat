package com.ashraf.repository;

import com.ashraf.entity.Rider;
import com.ashraf.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiderRepository extends JpaRepository<Rider, Long> {
    List<Rider> findByUser_Status(Status status);
}
