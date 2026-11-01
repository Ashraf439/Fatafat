package com.ashraf.customer.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class CustomerProfileResponse {
    private final Long id;
    private final String name;
    private final String email;
    private final String phoneNumber;
    private final LocalDate dateOfBirth;
    private final Integer loyaltyPoints;
    private final BigDecimal walletBalance;

    public CustomerProfileResponse(Long id, String name, String email, String phoneNumber,
                                   LocalDate dateOfBirth, Integer loyaltyPoints, BigDecimal walletBalance) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
        this.loyaltyPoints = loyaltyPoints;
        this.walletBalance = walletBalance;
    }
}