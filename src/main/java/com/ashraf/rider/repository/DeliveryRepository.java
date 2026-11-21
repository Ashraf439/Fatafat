package com.ashraf.rider.repository;

import com.ashraf.rider.entity.Delivery;
import com.ashraf.rider.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    boolean existsByOrder_Id(Long id);

    Optional<Delivery> findByOrder_PublicId(UUID publicId);
    Optional<Delivery> findFirstByRider_IdAndDeliveryStatusNot(Long riderId, DeliveryStatus excluded);
    Optional<Delivery> findFirstByRider_IdAndDeliveryStatusIn(Long riderId, Collection<DeliveryStatus> statuses);
}
