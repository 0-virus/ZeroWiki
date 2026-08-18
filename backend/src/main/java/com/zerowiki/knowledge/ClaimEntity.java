package com.zerowiki.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claims")
public class ClaimEntity {
    @Id
    private UUID id;

    @Column(name = "page_id", nullable = false)
    private UUID pageId;

    @Column(name = "claim_key", nullable = false, length = 120)
    private String claimKey;

    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    @Column(name = "ai_confidence", precision = 4, scale = 3)
    private BigDecimal aiConfidence;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClaimEntity() {
    }

    public static ClaimEntity create(UUID pageId, String claimKey, String statement,
                                      BigDecimal aiConfidence) {
        if (pageId == null || claimKey == null || claimKey.isBlank()
                || statement == null || statement.isBlank()) {
            throw new IllegalArgumentException("claim metadata is invalid");
        }
        ClaimEntity claim = new ClaimEntity();
        claim.id = UUID.randomUUID();
        claim.pageId = pageId;
        claim.claimKey = claimKey;
        claim.statement = statement;
        claim.aiConfidence = aiConfidence;
        return claim;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPageId() {
        return pageId;
    }

    public String getClaimKey() {
        return claimKey;
    }

    public String getStatement() {
        return statement;
    }

    public BigDecimal getAiConfidence() {
        return aiConfidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
