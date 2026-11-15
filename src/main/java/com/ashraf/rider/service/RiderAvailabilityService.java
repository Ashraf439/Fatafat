package com.ashraf.rider.service;

import com.ashraf.rider.entity.Rider;
import com.ashraf.rider.repository.RiderRepository;
import com.ashraf.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderAvailabilityService {

    private final RiderRepository riderRepository;
    private final RiderLocationService riderLocationService;

    public RiderAvailabilityService(RiderRepository riderRepository, RiderLocationService riderLocationService) {
        this.riderRepository = riderRepository;
        this.riderLocationService = riderLocationService;
    }

    @Transactional
    public void setOnline(Long riderId, double latitude, double longitude) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider not found"));
        rider.setIsAvailable(true);
        riderLocationService.updateLocation(riderId, latitude, longitude);
    }

    @Transactional
    public void setOffline(Long riderId) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider not found"));
        rider.setIsAvailable(false);
        riderLocationService.markUnavailable(riderId);
    }
}