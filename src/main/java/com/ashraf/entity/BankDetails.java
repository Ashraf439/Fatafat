package com.ashraf.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bank_details")
@Getter
@Setter
public class BankDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", unique = true, nullable = false)
    private Restaurant restaurant;

    private String accountHolderName;

    // TODO: encrypt at rest — see AttributeConverter decision, flagged not yet resolved
    private String accountNumber;

    private String ifscCode;

    private String bankName;
}