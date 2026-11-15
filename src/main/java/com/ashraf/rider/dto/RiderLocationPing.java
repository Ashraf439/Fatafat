package com.ashraf.rider.dto;

import java.util.UUID;

public record RiderLocationPing(double latitude, double longitude, UUID orderPublicId) {}