package com.zerowiki.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "change_reviews")
public class ChangeReviewEntity {
    @Id
    private UUID id;

    @Column(name = "change_set_id", nullable = false)
    private UUID changeSetId;

    @Column(name = "change_item_id")
    private UUID changeItemId;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Column(nullable = false, length = 30)
    private String decision;

    @Column(columnDefinition = "text")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChangeReviewEntity() {
    }

    private ChangeReviewEntity(UUID changeSetId, UUID changeItemId, UUID reviewerId,
                               String decision, String comment) {
        this.id = UUID.randomUUID();
        this.changeSetId = changeSetId;
        this.changeItemId = changeItemId;
        this.reviewerId = reviewerId;
        this.decision = decision;
        this.comment = comment;
    }

    static ChangeReviewEntity create(UUID changeSetId, UUID changeItemId, UUID reviewerId,
                                     String decision, String comment) {
        return new ChangeReviewEntity(changeSetId, changeItemId, reviewerId, decision, comment);
    }
}
