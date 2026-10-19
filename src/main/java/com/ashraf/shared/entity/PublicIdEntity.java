package com.ashraf.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

import java.util.UUID;

@MappedSuperclass
@Getter
public class PublicIdEntity {
    @Column(name = "publicId", unique = true, updatable = false)
    private UUID publicId;

    @PrePersist
    protected  void assignPublicId() {
        if(publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
