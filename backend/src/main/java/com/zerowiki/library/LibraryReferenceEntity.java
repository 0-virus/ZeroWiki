package com.zerowiki.library;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "library_references")
public class LibraryReferenceEntity {
    @Id
    private UUID id;

    @Column(name = "source_library_id", nullable = false)
    private UUID sourceLibraryId;

    @Column(name = "target_library_id", nullable = false)
    private UUID targetLibraryId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LibraryReferenceEntity() {
    }

    private LibraryReferenceEntity(UUID sourceLibraryId, UUID targetLibraryId, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.sourceLibraryId = sourceLibraryId;
        this.targetLibraryId = targetLibraryId;
        this.createdBy = createdBy;
    }

    public static LibraryReferenceEntity create(UUID sourceLibraryId, UUID targetLibraryId, UUID createdBy) {
        return new LibraryReferenceEntity(sourceLibraryId, targetLibraryId, createdBy);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceLibraryId() {
        return sourceLibraryId;
    }

    public UUID getTargetLibraryId() {
        return targetLibraryId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
