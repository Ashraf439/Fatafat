package com.ashraf.core.entity;

import com.ashraf.core.enums.PermissionEffect;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_permissions")
@Getter
@Setter
public class UserPermissions {
    @EmbeddedId
    private UserPermissionId id = new UserPermissionId();

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id")
    private Permissions permission;

    @Enumerated(EnumType.STRING)
    private PermissionEffect effect;
}
