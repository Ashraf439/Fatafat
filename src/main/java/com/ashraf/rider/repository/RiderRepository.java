package com.ashraf.rider.repository;

import com.ashraf.rider.entity.Rider;
import com.ashraf.core.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiderRepository extends JpaRepository<Rider, Long> {
    Optional<Rider> findByPublicId(UUID publicId);

    List<Rider> findByUser_Status(Status status);
}
