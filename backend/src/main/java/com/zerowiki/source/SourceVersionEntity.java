package com.zerowiki.source;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_versions")
public class SourceVersionEntity {
    @Id
    private UUID id;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "storage_key", nullable = false, columnDefinition = "text")
    private String storageKey;

    @Column(name = "media_type", nullable = false, length = 120)
    private String mediaType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "extracted_text_key", columnDefinition = "text")
    private String extractedTextKey;

    @Column(name = "detected_language", length = 10)
    private String detectedLanguage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_metadata", nullable = false, columnDefinition = "jsonb")
    private JsonNode sourceMetadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "capture_method", nullable = false, length = 30)
    private CaptureMethod captureMethod;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SourceVersionEntity() {
    }

    private SourceVersionEntity(UUID sourceId, int versionNo, String contentHash, String storageKey,
                                String mediaType, long byteSize, JsonNode sourceMetadata,
                                CaptureMethod captureMethod, Instant capturedAt) {
        this.id = UUID.randomUUID();
        this.sourceId = sourceId;
        this.versionNo = versionNo;
        this.contentHash = contentHash;
        this.storageKey = storageKey;
        this.mediaType = mediaType;
        this.byteSize = byteSize;
        this.sourceMetadata = sourceMetadata == null ? null : sourceMetadata.deepCopy();
        this.captureMethod = captureMethod;
        this.capturedAt = capturedAt;
    }

    public static SourceVersionEntity create(UUID sourceId, int versionNo, String contentHash, String storageKey,
                                             String mediaType, long byteSize, JsonNode sourceMetadata,
                                             CaptureMethod captureMethod) {
        if (sourceId == null || versionNo < 1 || contentHash == null || contentHash.isBlank()
                || storageKey == null || storageKey.isBlank() || mediaType == null || mediaType.isBlank()
                || byteSize < 0 || captureMethod == null) {
            throw new IllegalArgumentException("source version metadata is invalid");
        }
        return new SourceVersionEntity(sourceId, versionNo, contentHash, storageKey, mediaType, byteSize,
                sourceMetadata, captureMethod, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getMediaType() {
        return mediaType;
    }

    public long getByteSize() {
        return byteSize;
    }

    public String getExtractedTextKey() {
        return extractedTextKey;
    }

    public String getDetectedLanguage() {
        return detectedLanguage;
    }

    public JsonNode getSourceMetadata() {
        return sourceMetadata == null ? null : sourceMetadata.deepCopy();
    }

    public CaptureMethod getCaptureMethod() {
        return captureMethod;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
