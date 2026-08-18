package com.zerowiki.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

@Entity
@Table(name = "change_sets")
public class ChangeSetEntity {
    @Id
    private UUID id;

    @Column(name = "library_id", nullable = false)
    private UUID libraryId;

    @Column(name = "origin_type", nullable = false, length = 30)
    private String originType;

    @Column(name = "origin_id")
    private UUID originId;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private long revision;

    @Column(name = "base_library_version_id")
    private UUID baseLibraryVersionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    protected ChangeSetEntity() {
    }

    private ChangeSetEntity(UUID libraryId, String originType, UUID originId, String title, String summary,
                            String riskLevel, String status) {
        this.id = UUID.randomUUID();
        this.libraryId = libraryId;
        this.originType = originType;
        this.originId = originId;
        this.title = title;
        this.summary = summary;
        this.riskLevel = riskLevel;
        this.status = status;
        this.revision = 1;
    }

    public static ChangeSetEntity ready(UUID libraryId, String originType, UUID originId, String title, String summary,
                                        String riskLevel) {
        return new ChangeSetEntity(libraryId, originType, originId, title, summary, riskLevel, "READY_FOR_REVIEW");
    }

    public UUID getId() {
        return id;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public String getOriginType() {
        return originType;
    }

    public UUID getOriginId() {
        return originId;
    }

    public UUID getBaseLibraryVersionId() {
        return baseLibraryVersionId;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getStatus() {
        return status;
    }

    public long getRevision() {
        return revision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    void refreshReviewStatus(Collection<ChangeItemEntity> items) {
        boolean anyRevision = items.stream().anyMatch(item -> "REVISION_REQUESTED".equals(item.getReviewStatus()));
        boolean anyPending = items.stream().anyMatch(item -> "PENDING".equals(item.getReviewStatus())
                || "DEFERRED".equals(item.getReviewStatus()));
        boolean allApproved = !items.isEmpty() && items.stream()
                .allMatch(item -> "APPROVED".equals(item.getReviewStatus()));
        boolean allRejected = !items.isEmpty() && items.stream()
                .allMatch(item -> "REJECTED".equals(item.getReviewStatus()));
        if (allApproved) {
            status = "APPROVED";
        } else if (allRejected) {
            status = "REJECTED";
        } else if (anyRevision || anyPending) {
            status = "PARTIALLY_APPROVED";
        } else {
            status = "PARTIALLY_APPROVED";
        }
        reviewedAt = Instant.now();
        revision++;
    }

    void recalculate(UUID currentLibraryVersionId) {
        this.baseLibraryVersionId = currentLibraryVersionId;
        this.status = "READY_FOR_REVIEW";
        this.revision++;
    }

    void markApplied() {
        if (!"APPROVED".equals(status) && !"PARTIALLY_APPROVED".equals(status)) {
            throw new IllegalStateException("change set is not approved");
        }
        status = "APPLIED";
        appliedAt = Instant.now();
        revision++;
    }
}
