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
@Table(name = "contradictions")
public class ContradictionEntity {
    @Id
    private UUID id;

    @Column(name = "library_id", nullable = false)
    private UUID libraryId;

    @Column(name = "left_claim_id", nullable = false)
    private UUID leftClaimId;

    @Column(name = "right_claim_id", nullable = false)
    private UUID rightClaimId;

    @Column(nullable = false, length = 30)
    private String classification;

    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "resolution_note", columnDefinition = "text")
    private String resolutionNote;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ContradictionEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public UUID getLeftClaimId() {
        return leftClaimId;
    }

    public UUID getRightClaimId() {
        return rightClaimId;
    }

    public String getClassification() {
        return classification;
    }

    public String getExplanation() {
        return explanation;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getStatus() {
        return status;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public UUID getResolvedBy() {
        return resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    void resolve(String nextStatus, String nextClassification, String note, UUID actorId) {
        if (nextStatus == null || nextStatus.isBlank() || actorId == null) {
            throw new IllegalArgumentException("contradiction resolution metadata is invalid");
        }
        this.status = nextStatus;
        if (nextClassification != null && !nextClassification.isBlank()) {
            this.classification = nextClassification;
        }
        this.resolutionNote = note;
        this.resolvedBy = actorId;
        this.resolvedAt = Instant.now();
    }
}
