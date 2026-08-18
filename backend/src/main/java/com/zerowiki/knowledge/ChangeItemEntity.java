package com.zerowiki.knowledge;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "change_items")
public class ChangeItemEntity {
    @Id
    private UUID id;

    @Column(name = "change_set_id", nullable = false)
    private UUID changeSetId;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(nullable = false, length = 20)
    private String operation;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(columnDefinition = "text")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_summary", columnDefinition = "jsonb")
    private JsonNode evidenceSummary;

    @Column(name = "ai_confidence", precision = 4, scale = 3)
    private java.math.BigDecimal aiConfidence;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_snapshot", columnDefinition = "jsonb")
    private JsonNode beforeSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_snapshot", columnDefinition = "jsonb")
    private JsonNode afterSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChangeItemEntity() {
    }

    private ChangeItemEntity(UUID changeSetId, String entityType, UUID entityId, String operation,
                             JsonNode beforeSnapshot, JsonNode afterSnapshot) {
        this.id = UUID.randomUUID();
        this.changeSetId = changeSetId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.operation = operation;
        this.riskLevel = "MINOR";
        this.reviewStatus = "PENDING";
        this.beforeSnapshot = beforeSnapshot == null ? null : beforeSnapshot.deepCopy();
        this.afterSnapshot = afterSnapshot == null ? null : afterSnapshot.deepCopy();
    }

    public static ChangeItemEntity update(UUID changeSetId, String entityType, UUID entityId,
                                          JsonNode beforeSnapshot, JsonNode afterSnapshot) {
        return new ChangeItemEntity(changeSetId, entityType, entityId, "UPDATE", beforeSnapshot, afterSnapshot);
    }

    public static ChangeItemEntity create(UUID changeSetId, String entityType, UUID entityId,
                                          JsonNode afterSnapshot) {
        return new ChangeItemEntity(changeSetId, entityType, entityId, "CREATE", null, afterSnapshot);
    }

    public UUID getId() {
        return id;
    }

    public UUID getChangeSetId() {
        return changeSetId;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getOperation() {
        return operation;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getReason() {
        return reason;
    }

    public JsonNode getEvidenceSummary() {
        return evidenceSummary == null ? null : evidenceSummary.deepCopy();
    }

    public java.math.BigDecimal getAiConfidence() {
        return aiConfidence;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public JsonNode getBeforeSnapshot() {
        return beforeSnapshot == null ? null : beforeSnapshot.deepCopy();
    }

    public JsonNode getAfterSnapshot() {
        return afterSnapshot == null ? null : afterSnapshot.deepCopy();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    void review(String decision) {
        reviewStatus = switch (decision) {
            case "APPROVE" -> "APPROVED";
            case "REJECT" -> "REJECTED";
            case "REQUEST_CHANGES" -> "REVISION_REQUESTED";
            case "DEFER" -> "DEFERRED";
            default -> throw new IllegalArgumentException("unsupported review decision");
        };
    }
}
