package com.ashraf.restaurant.onboarding.dto;

import lombok.Getter;

/** Drives the "Add restaurant" UI: has this owner paid the one-time fee, and how much of their quota is used. */
@Getter
public class OwnerEntitlementResponse {
    private final boolean paid;
    private final int quota;
    private final long used;

    public OwnerEntitlementResponse(boolean paid, int quota, long used) {
        this.paid = paid;
        this.quota = quota;
        this.used = used;
    }
}