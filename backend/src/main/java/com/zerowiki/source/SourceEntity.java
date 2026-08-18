package com.zerowiki.source;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sources")
public class SourceEntity {
    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType;

    @Column(name = "canonical_uri", nullable = false, columnDefinition = "text")
    private String canonicalUri;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SourceStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SourceEntity() {
    }

    private SourceEntity(UUID id, UUID ownerId, SourceType sourceType, String canonicalUri, String title) {
        this.id = id;
        this.ownerId = ownerId;
        this.sourceType = sourceType;
        this.canonicalUri = canonicalUri;
        this.title = title;
        this.status = SourceStatus.ACTIVE;
    }

    public static SourceEntity create(UUID ownerId, SourceType sourceType, String canonicalUri, String title) {
        if (ownerId == null || sourceType == null || canonicalUri == null || canonicalUri.isBlank()
                || title == null || title.isBlank()) {
            throw new IllegalArgumentException("source identity is required");
        }
        return new SourceEntity(UUID.randomUUID(), ownerId, sourceType, canonicalUri, title.trim());
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getCanonicalUri() {
        return canonicalUri;
    }

    public String getTitle() {
        return title;
    }

    public SourceStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void rename(String title) {
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
        }
    }

    public void markDeletionPending() {
        this.status = SourceStatus.DELETION_PENDING;
    }
}
