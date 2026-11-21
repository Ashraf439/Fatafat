package com.ashraf.rider.dto;

import com.ashraf.rider.entity.Rider;
import java.util.UUID;

/**
 * jobsTopic is the STOMP destination the rider app subscribes to for new assignments
 * (DeliveryChannelInterceptor only lets the owning rider subscribe to it).
 */
public record RiderProfileResponse(UUID publicId, String name, String phoneNumber, String email,
                                   String vehicleType, String vehicleNumber, String vehicleModel,
                                   String vehicleColor, boolean isAvailable, String operatingCity,
                                   String verificationStatus, String rejectionReason, String jobsTopic) {
    public static RiderProfileResponse from(Rider rider) {
        return new RiderProfileResponse(
                rider.getPublicId(), rider.getName(), rider.getUser().getPhoneNumber(), rider.getUser().getEmail(),
                rider.getVehicleType(), rider.getVehicleNumber(), rider.getVehicleModel(), rider.getVehicleColor(),
                Boolean.TRUE.equals(rider.getIsAvailable()), rider.getOperatingCity(),
                rider.getVerificationStatus() != null ? rider.getVerificationStatus().name() : null,
                rider.getRejectionReason(),
                "/topic/rider/" + rider.getId() + "/jobs");
    }
}
