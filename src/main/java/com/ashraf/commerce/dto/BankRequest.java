package com.ashraf.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankRequest {
    @NotBlank
    private String accountHolderName;

    @NotBlank
    @Pattern(regexp = "^\\d{9,18}$")
    private String accountNumber;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format. Example: HDFC0001234")
    private String ifscCode;


    @NotBlank
    private String bankName;
}
