package com.ashraf.rider.dto;

import java.math.BigDecimal;

public record DeliveryTrackingResponse(String status, String riderName, String riderPhoneNumber,
                                       String vehicleType, String vehicleNumber,
                                       BigDecimal pickupLatitude, BigDecimal pickupLongitude,
                                       BigDecimal dropLatitude, BigDecimal dropLongitude,
                                       String locationTopic, Boolean otpRequired, String deliveryOtp) {}