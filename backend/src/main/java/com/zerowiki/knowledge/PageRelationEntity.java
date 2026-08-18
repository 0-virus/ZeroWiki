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
@Table(name = "page_relations")
public class PageRelationEntity {
    @Id
    private UUID id;

    @Column(name = "library_id", nullable = false)
    private UUID libraryId;

    @Column(name = "source_page_id", nullable = false)
    private UUID sourcePageId;

    @Column(name = "target_page_id", nullable = false)
    private UUID targetPageId;

    @Column(name = "relation_type", nullable = false, length = 40)
    private String relationType;

    @Column(columnDefinition = "text")
    private String rationale;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_by_type", nullable = false, length = 20)
    private String createdByType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PageRelationEntity() {
    }

    public static PageRelationEntity imported(UUID libraryId, UUID sourcePageId, UUID targetPageId,
                                               String relationType, String rationale,
                                               BigDecimal confidence) {
        return create(libraryId, sourcePageId, targetPageId, relationType, rationale, confidence,
                "ACCEPTED", "IMPORT");
    }

    public static PageRelationEntity proposed(UUID libraryId, UUID sourcePageId, UUID targetPageId,
                                               String relationType, String rationale,
                                               BigDecimal confidence) {
        return create(libraryId, sourcePageId, targetPageId, relationType, rationale, confidence,
                "PROPOSED", "IMPORT");
    }

    private static PageRelationEntity create(UUID libraryId, UUID sourcePageId, UUID targetPageId,
                                             String relationType, String rationale,
                                             BigDecimal confidence, String status, String createdByType) {
        if (libraryId == null || sourcePageId == null || targetPageId == null
                || sourcePageId.equals(targetPageId) || relationType == null || relationType.isBlank()) {
            throw new IllegalArgumentException("relation metadata is invalid");
        }
        PageRelationEntity relation = new PageRelationEntity();
        relation.id = UUID.randomUUID();
        relation.libraryId = libraryId;
        relation.sourcePageId = sourcePageId;
        relation.targetPageId = targetPageId;
        relation.relationType = relationType;
        relation.rationale = rationale;
        relation.confidence = confidence;
        relation.status = status;
        relation.createdByType = createdByType;
        return relation;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public UUID getSourcePageId() {
        return sourcePageId;
    }

    public UUID getTargetPageId() {
        return targetPageId;
    }

    public String getRelationType() {
        return relationType;
    }

    public String getRationale() {
        return rationale;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedByType() {
        return createdByType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    void reject() {
        this.status = "REJECTED";
    }

    void accept() {
        this.status = "ACCEPTED";
    }

    void archive() {
        this.status = "ARCHIVED";
    }
}
