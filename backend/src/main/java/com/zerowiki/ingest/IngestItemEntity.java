package com.zerowiki.ingest;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingest_items")
public class IngestItemEntity {
    @Id
    private UUID id;

    @Column(name = "ingest_job_id", nullable = false)
    private UUID ingestJobId;

    @Column(name = "source_version_id", nullable = false)
    private UUID sourceVersionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IngestItemStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topic_hints", nullable = false, columnDefinition = "jsonb")
    private JsonNode topicHints;

    @Column(name = "duplicate_of_source_id")
    private UUID duplicateOfSourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sensitivity_flags", nullable = false, columnDefinition = "jsonb")
    private JsonNode sensitivityFlags;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "failure_message", columnDefinition = "text")
    private String failureMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IngestItemEntity() {
    }

    private IngestItemEntity(UUID ingestJobId, UUID sourceVersionId, JsonNode emptyArray) {
        this.id = UUID.randomUUID();
        this.ingestJobId = ingestJobId;
        this.sourceVersionId = sourceVersionId;
        this.status = IngestItemStatus.PENDING;
        this.topicHints = emptyArray.deepCopy();
        this.sensitivityFlags = emptyArray.deepCopy();
    }

    public static IngestItemEntity create(UUID ingestJobId, UUID sourceVersionId, JsonNode emptyArray) {
        if (ingestJobId == null || sourceVersionId == null || emptyArray == null) {
            throw new IllegalArgumentException("ingest item metadata is invalid");
        }
        return new IngestItemEntity(ingestJobId, sourceVersionId, emptyArray);
    }

    public UUID getId() {
        return id;
    }

    public UUID getIngestJobId() {
        return ingestJobId;
    }

    public UUID getSourceVersionId() {
        return sourceVersionId;
    }

    public IngestItemStatus getStatus() {
        return status;
    }

    public JsonNode getTopicHints() {
        return topicHints == null ? null : topicHints.deepCopy();
    }

    public UUID getDuplicateOfSourceId() {
        return duplicateOfSourceId;
    }

    public JsonNode getSensitivityFlags() {
        return sensitivityFlags == null ? null : sensitivityFlags.deepCopy();
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markDone() {
        this.status = IngestItemStatus.DONE;
        this.failureCode = null;
        this.failureMessage = null;
    }

    public void markFailed(String code, String message) {
        this.status = IngestItemStatus.FAILED;
        this.failureCode = code;
        this.failureMessage = message;
    }

    public void resetForRetry() {
        if (status != IngestItemStatus.FAILED) {
            throw new IllegalStateException("only failed items can be retried");
        }
        this.status = IngestItemStatus.PENDING;
        this.retryCount++;
        this.failureCode = null;
        this.failureMessage = null;
    }
}
