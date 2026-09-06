package com.ashraf.restaurant.staff.dto;

import com.ashraf.core.enums.PermissionEffect;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffPermissionRequest {
    @NotBlank
    private String permissionName;
    @NotBlank
    private PermissionEffect effect;
}
