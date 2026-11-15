package com.ashraf.rider.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record JobOfferResponse(UUID orderPublicId, String restaurantName,
                               BigDecimal pickupLatitude, BigDecimal pickupLongitude,
                               BigDecimal dropLatitude, BigDecimal dropLongitude) {}