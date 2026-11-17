package com.ashraf.rider.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectRiderRequest {
    @NotBlank
    private String rejectionReason;
}