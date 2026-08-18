package com.zerowiki.knowledge;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "claim_statuses")
public class ClaimStatusEntity {
    @Id
    private UUID id;

    @Column(name = "claim_id", nullable = false)
    private UUID claimId;

    @Column(name = "page_version_id", nullable = false)
    private UUID pageVersionId;

    @Column(name = "knowledge_status", nullable = false, length = 30)
    private String knowledgeStatus;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode conditions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClaimStatusEntity() {
    }

    public static ClaimStatusEntity create(UUID claimId, UUID pageVersionId, String knowledgeStatus,
                                            BigDecimal confidence, JsonNode conditions) {
        if (claimId == null || pageVersionId == null || knowledgeStatus == null || knowledgeStatus.isBlank()) {
            throw new IllegalArgumentException("claim status metadata is invalid");
        }
        ClaimStatusEntity status = new ClaimStatusEntity();
        status.id = UUID.randomUUID();
        status.claimId = claimId;
        status.pageVersionId = pageVersionId;
        status.knowledgeStatus = knowledgeStatus;
        status.confidence = confidence;
        status.conditions = conditions == null ? new tools.jackson.databind.ObjectMapper().createObjectNode()
                : conditions.deepCopy();
        return status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClaimId() {
        return claimId;
    }

    public UUID getPageVersionId() {
        return pageVersionId;
    }

    public String getKnowledgeStatus() {
        return knowledgeStatus;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public JsonNode getConditions() {
        return conditions == null ? null : conditions.deepCopy();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
