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

    /** The only @PrePersist in the hierarchy: JPA entities extending this class must not declare their own. */
    @PrePersist
    protected void assignPublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        onPublicIdAssigned();
    }

    /** Hook for subclasses that derive something from the public id (e.g. a slug). Runs right after it is set. */
    protected void onPublicIdAssigned() {
        // nothing by default
    }
}
