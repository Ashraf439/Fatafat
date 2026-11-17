package com.ashraf.rider.dto;

import com.ashraf.rider.enums.RiderVerificationStatus;
import java.util.UUID;

public record RiderSummaryResponse(UUID publicId, String name, String email, String phoneNumber,
                                   String vehicleType, String vehicleNumber,
                                   RiderVerificationStatus verificationStatus) {}