package com.zerowiki.assistant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class ConversationEntity {
    @Id
    private UUID id;

    @Column(name = "library_id", nullable = false)
    private UUID libraryId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConversationEntity() {
    }

    public static ConversationEntity create(UUID libraryId, UUID userId, String title) {
        if (libraryId == null || userId == null) {
            throw new IllegalArgumentException("conversation ownership is required");
        }
        ConversationEntity conversation = new ConversationEntity();
        conversation.id = UUID.randomUUID();
        conversation.libraryId = libraryId;
        conversation.userId = userId;
        conversation.title = title == null || title.isBlank() ? "새 대화" : title.trim();
        conversation.status = "ACTIVE";
        return conversation;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
