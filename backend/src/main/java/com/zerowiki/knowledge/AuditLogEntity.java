package com.zerowiki.knowledge;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    @Id
    private UUID id;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "library_id")
    private UUID libraryId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "resource_type", nullable = false, length = 30)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(nullable = false, length = 20)
    private String result;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode metadata;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLogEntity() {
    }

    private AuditLogEntity(UUID actorUserId, UUID libraryId, String eventType, String resourceType,
                           UUID resourceId, String result, JsonNode metadata) {
        this.id = UUID.randomUUID();
        this.actorUserId = actorUserId;
        this.libraryId = libraryId;
        this.eventType = eventType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.result = result;
        this.metadata = metadata == null ? null : metadata.deepCopy();
    }

    static AuditLogEntity success(UUID actorUserId, UUID libraryId, String resourceType, UUID resourceId,
                                  JsonNode metadata) {
        return new AuditLogEntity(actorUserId, libraryId, "CHANGE_APPLIED", resourceType,
                resourceId, "SUCCESS", metadata);
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getResult() {
        return result;
    }

    public JsonNode getMetadata() {
        return metadata == null ? null : metadata.deepCopy();
    }

    public String getRequestId() {
        return requestId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
