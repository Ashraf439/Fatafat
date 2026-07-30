package com.ashraf.restaurant.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectApplicationRequest {

    @NotBlank
    private String rejectionReason;

}