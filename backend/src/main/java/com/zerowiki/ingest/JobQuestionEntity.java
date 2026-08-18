package com.zerowiki.ingest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_questions")
public class JobQuestionEntity {
    @Id
    private UUID id;

    @Column(name = "ingest_job_id", nullable = false)
    private UUID ingestJobId;

    @Column(name = "ingest_item_id")
    private UUID ingestItemId;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(columnDefinition = "text")
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private QuestionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "answered_at")
    private Instant answeredAt;

    protected JobQuestionEntity() {
    }

    public static JobQuestionEntity create(UUID ingestJobId, UUID ingestItemId, String question) {
        if (ingestJobId == null || question == null || question.isBlank()) {
            throw new IllegalArgumentException("question metadata is invalid");
        }
        JobQuestionEntity result = new JobQuestionEntity();
        result.id = UUID.randomUUID();
        result.ingestJobId = ingestJobId;
        result.ingestItemId = ingestItemId;
        result.question = question.trim();
        result.status = QuestionStatus.OPEN;
        return result;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIngestJobId() {
        return ingestJobId;
    }

    public UUID getIngestItemId() {
        return ingestItemId;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void answer(String answer) {
        if (status != QuestionStatus.OPEN) {
            throw new IllegalStateException("question is already resolved");
        }
        this.answer = answer;
        this.status = QuestionStatus.ANSWERED;
        this.answeredAt = Instant.now();
    }

    public void skip() {
        if (status != QuestionStatus.OPEN) {
            throw new IllegalStateException("question is already resolved");
        }
        this.status = QuestionStatus.SKIPPED;
        this.answeredAt = Instant.now();
    }
}
