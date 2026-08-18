package com.zerowiki.assistant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feedback_events")
public class FeedbackEventEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "library_id", nullable = false)
    private UUID libraryId;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "feedback_type", nullable = false, length = 40)
    private String feedbackType;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(nullable = false, length = 20)
    private String scope;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FeedbackEventEntity() {
    }

    public static FeedbackEventEntity answer(UUID userId, UUID libraryId, UUID messageId,
                                             String feedbackType, String comment) {
        FeedbackEventEntity event = new FeedbackEventEntity();
        event.id = UUID.randomUUID();
        event.userId = userId;
        event.libraryId = libraryId;
        event.targetType = "ANSWER";
        event.targetId = messageId;
        event.feedbackType = feedbackType;
        event.comment = comment;
        event.scope = "LIBRARY";
        return event;
    }
}
