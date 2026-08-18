package com.zerowiki.source;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upload_sessions")
public class UploadSessionEntity {
    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "media_type", nullable = false, length = 120)
    private String mediaType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "expected_sha256", nullable = false, length = 64)
    private String expectedSha256;

    @Column(name = "upload_token_hash", nullable = false, length = 64, unique = true)
    private String uploadTokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "storage_key", nullable = false, columnDefinition = "text")
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private UploadStatus status;

    @Column(name = "uploaded_byte_size")
    private Long uploadedByteSize;

    @Column(name = "uploaded_sha256", length = 64)
    private String uploadedSha256;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected UploadSessionEntity() {
    }

    private UploadSessionEntity(UUID ownerId, String fileName, String mediaType, long byteSize,
                                String expectedSha256, String uploadTokenHash, Instant expiresAt,
                                String storageKey) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.byteSize = byteSize;
        this.expectedSha256 = expectedSha256;
        this.uploadTokenHash = uploadTokenHash;
        this.expiresAt = expiresAt;
        this.storageKey = storageKey;
        this.status = UploadStatus.PLANNED;
    }

    public static UploadSessionEntity plan(UUID ownerId, String fileName, String mediaType, long byteSize,
                                           String expectedSha256, String uploadTokenHash, Instant expiresAt,
                                           String storageKey) {
        return new UploadSessionEntity(ownerId, fileName, mediaType, byteSize, expectedSha256,
                uploadTokenHash, expiresAt, storageKey);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMediaType() {
        return mediaType;
    }

    public long getByteSize() {
        return byteSize;
    }

    public String getExpectedSha256() {
        return expectedSha256;
    }

    public String getUploadTokenHash() {
        return uploadTokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public Long getUploadedByteSize() {
        return uploadedByteSize;
    }

    public String getUploadedSha256() {
        return uploadedSha256;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void markUploaded(long size, String sha256) {
        this.uploadedByteSize = size;
        this.uploadedSha256 = sha256;
        this.status = UploadStatus.UPLOADED;
    }

    public void markCompleted() {
        this.status = UploadStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markCancelled() {
        this.status = UploadStatus.CANCELLED;
    }

    public void markBlockedSecret() {
        this.status = UploadStatus.BLOCKED_SECRET;
    }
}
