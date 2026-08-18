package com.zerowiki.ingest;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingest_jobs")
public class IngestJobEntity {
    @Id
    private UUID id;

    @Column(name = "library_id", nullable = false)
    private UUID libraryId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_type", nullable = false, length = 24)
    private ImportType importType;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_mode", nullable = false, length = 12)
    private ProcessingMode processingMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private IngestStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode plan;

    @Version
    @Column(name = "plan_revision", nullable = false)
    private long planRevision;

    @Column(name = "estimated_tokens", nullable = false)
    private long estimatedTokens;

    @Column(name = "actual_tokens")
    private Long actualTokens;

    @Column(name = "estimated_cost", precision = 18, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 18, scale = 6)
    private BigDecimal actualCost;

    @Column(name = "progress_total", nullable = false)
    private int progressTotal;

    @Column(name = "progress_completed", nullable = false)
    private int progressCompleted;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "failure_message", columnDefinition = "text")
    private String failureMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IngestJobEntity() {
    }

    private IngestJobEntity(UUID libraryId, UUID requestedBy, ImportType importType,
                            ProcessingMode processingMode, JsonNode plan, long estimatedTokens,
                            int progressTotal) {
        this.id = UUID.randomUUID();
        this.libraryId = libraryId;
        this.requestedBy = requestedBy;
        this.importType = importType;
        this.processingMode = processingMode;
        this.status = IngestStatus.SCANNING;
        this.plan = plan == null ? null : plan.deepCopy();
        this.planRevision = 1;
        this.estimatedTokens = estimatedTokens;
        this.progressTotal = progressTotal;
        this.progressCompleted = 0;
        this.startedAt = Instant.now();
    }

    public static IngestJobEntity create(UUID libraryId, UUID requestedBy, ImportType importType,
                                         ProcessingMode processingMode, JsonNode plan, long estimatedTokens,
                                         int progressTotal) {
        if (libraryId == null || requestedBy == null || importType == null || processingMode == null
                || estimatedTokens < 0 || progressTotal < 0) {
            throw new IllegalArgumentException("ingest job metadata is invalid");
        }
        return new IngestJobEntity(libraryId, requestedBy, importType, processingMode, plan,
                estimatedTokens, progressTotal);
    }

    public UUID getId() {
        return id;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public ImportType getImportType() {
        return importType;
    }

    public ProcessingMode getProcessingMode() {
        return processingMode;
    }

    public IngestStatus getStatus() {
        return status;
    }

    public JsonNode getPlan() {
        return plan == null ? null : plan.deepCopy();
    }

    public long getPlanRevision() {
        return planRevision;
    }

    public long getEstimatedTokens() {
        return estimatedTokens;
    }

    public Long getActualTokens() {
        return actualTokens;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public BigDecimal getActualCost() {
        return actualCost;
    }

    public int getProgressTotal() {
        return progressTotal;
    }

    public int getProgressCompleted() {
        return progressCompleted;
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

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void moveToPlanReview(JsonNode nextPlan, long nextEstimatedTokens) {
        requireStatus(IngestStatus.SCANNING);
        this.status = IngestStatus.PLAN_REVIEW;
        this.plan = nextPlan == null ? null : nextPlan.deepCopy();
        this.estimatedTokens = nextEstimatedTokens;
    }

    public void approvePlan() {
        requireStatus(IngestStatus.PLAN_REVIEW);
        this.status = IngestStatus.PROCESSING;
    }

    public void rejectPlan() {
        requireStatus(IngestStatus.PLAN_REVIEW);
        this.status = IngestStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    public void enterQuestionReview() {
        requireStatus(IngestStatus.PROCESSING);
        this.status = IngestStatus.QUESTION_REVIEW;
    }

    public void resumeAfterQuestions() {
        requireStatus(IngestStatus.QUESTION_REVIEW);
        this.status = IngestStatus.PROCESSING;
    }

    public void moveToChangeReview() {
        requireStatus(IngestStatus.PROCESSING);
        this.status = IngestStatus.CHANGE_REVIEW;
    }

    public void complete() {
        requireStatus(IngestStatus.CHANGE_REVIEW);
        this.status = IngestStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void cancel() {
        if (status == IngestStatus.COMPLETED || status == IngestStatus.CANCELLED) {
            throw invalidTransition();
        }
        this.status = IngestStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    public void retry() {
        if (status != IngestStatus.FAILED) {
            throw invalidTransition();
        }
        this.status = IngestStatus.PROCESSING;
        this.failureCode = null;
        this.failureMessage = null;
    }

    public void markProgress(int completed) {
        if (completed < 0 || completed > progressTotal) {
            throw new IllegalArgumentException("progress is out of range");
        }
        this.progressCompleted = completed;
    }

    private void requireStatus(IngestStatus expected) {
        if (status != expected) {
            throw invalidTransition();
        }
    }

    private IllegalStateException invalidTransition() {
        return new IllegalStateException("invalid ingest state transition: " + status);
    }
}
