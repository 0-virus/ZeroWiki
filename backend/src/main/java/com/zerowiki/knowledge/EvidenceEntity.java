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
import java.util.UUID;

@Entity
@Table(name = "evidences")
public class EvidenceEntity {
    @Id
    private UUID id;

    @Column(name = "claim_id", nullable = false)
    private UUID claimId;

    @Column(name = "source_version_id", nullable = false)
    private UUID sourceVersionId;

    @Column(name = "evidence_type", nullable = false, length = 20)
    private String evidenceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode locator;

    @Column(nullable = false, columnDefinition = "text")
    private String excerpt;

    @Column(name = "excerpt_language", nullable = false, length = 20)
    private String excerptLanguage;

    @Column(name = "source_trust", precision = 4, scale = 3)
    private BigDecimal sourceTrust;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EvidenceEntity() {
    }

    public static EvidenceEntity create(UUID claimId, UUID sourceVersionId, String evidenceType,
                                         JsonNode locator, String excerpt, String excerptLanguage,
                                         BigDecimal sourceTrust) {
        if (claimId == null || sourceVersionId == null || evidenceType == null || evidenceType.isBlank()
                || locator == null || excerpt == null || excerpt.length() > 400
                || excerptLanguage == null || excerptLanguage.isBlank()) {
            throw new IllegalArgumentException("evidence metadata is invalid");
        }
        EvidenceEntity evidence = new EvidenceEntity();
        evidence.id = UUID.randomUUID();
        evidence.claimId = claimId;
        evidence.sourceVersionId = sourceVersionId;
        evidence.evidenceType = evidenceType;
        evidence.locator = locator.deepCopy();
        evidence.excerpt = excerpt;
        evidence.excerptLanguage = excerptLanguage;
        evidence.sourceTrust = sourceTrust;
        return evidence;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClaimId() {
        return claimId;
    }

    public UUID getSourceVersionId() {
        return sourceVersionId;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public JsonNode getLocator() {
        return locator == null ? null : locator.deepCopy();
    }

    public String getExcerpt() {
        return excerpt;
    }

    public String getExcerptLanguage() {
        return excerptLanguage;
    }

    public BigDecimal getSourceTrust() {
        return sourceTrust;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
