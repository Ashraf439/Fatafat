package com.ashraf.rider.repository;

import com.ashraf.rider.entity.Rider;
import com.ashraf.core.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiderRepository extends JpaRepository<Rider, Long> {
    List<Rider> findByUser_Status(Status status);
}
