package com.ashraf.rider.dto;

import com.ashraf.rider.entity.Rider;
import java.util.UUID;

public record RiderProfileResponse(UUID publicId, String name, String phoneNumber, String email,
                                   String vehicleType, String vehicleNumber, String vehicleModel,
                                   String vehicleColor, boolean isAvailable, String operatingCity) {
    public static RiderProfileResponse from(Rider rider) {
        return new RiderProfileResponse(
                rider.getPublicId(), rider.getName(), rider.getUser().getPhoneNumber(), rider.getUser().getEmail(),
                rider.getVehicleType(), rider.getVehicleNumber(), rider.getVehicleModel(), rider.getVehicleColor(),
                Boolean.TRUE.equals(rider.getIsAvailable()), rider.getOperatingCity());
    }
}