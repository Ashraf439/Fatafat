package com.ashraf.rider.entity;

import com.ashraf.core.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "riders")
@Getter
@Setter
public class Rider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private String name;

    private Boolean isAvailable;

    private String vehicleType;

    private String vehicleNumber;

    private String vehicleModel;

    private String vehicleColor;

    private String drivingLicenseNumber;

    private String nationalId;

    private String operatingCity;

    private String referralCode;
}