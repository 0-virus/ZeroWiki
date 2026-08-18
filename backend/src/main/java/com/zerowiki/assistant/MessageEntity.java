package com.zerowiki.assistant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class MessageEntity {
    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "answer_mode", length = 30)
    private String answerMode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "insufficient_evidence", nullable = false)
    private boolean insufficientEvidence;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected MessageEntity() {
    }

    private MessageEntity(UUID conversationId, String role, String content, String answerMode, String status) {
        this.id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.role = role;
        this.content = content == null ? "" : content;
        this.answerMode = answerMode;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public static MessageEntity user(UUID conversationId, String content, String answerMode) {
        if (conversationId == null || content == null || content.isBlank()) {
            throw new IllegalArgumentException("user message is required");
        }
        return new MessageEntity(conversationId, "USER", content.trim(), answerMode, "COMPLETED");
    }

    public static MessageEntity assistant(UUID conversationId, String answerMode) {
        if (conversationId == null) {
            throw new IllegalArgumentException("conversation is required");
        }
        return new MessageEntity(conversationId, "ASSISTANT", "", answerMode, "QUEUED");
    }

    public void complete(String answer, boolean insufficientEvidence, int tokenUsage) {
        this.content = answer == null ? "" : answer;
        this.insufficientEvidence = insufficientEvidence;
        this.tokenUsage = Math.max(0, tokenUsage);
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getAnswerMode() {
        return answerMode;
    }

    public String getStatus() {
        return status;
    }

    public boolean isInsufficientEvidence() {
        return insufficientEvidence;
    }

    public Integer getTokenUsage() {
        return tokenUsage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
