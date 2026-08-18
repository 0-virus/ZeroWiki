package com.zerowiki.assistant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecordEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "request_path", nullable = false, columnDefinition = "text")
    private String requestPath;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_body_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status")
    private Integer responseHttpStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private JsonNode responseBody;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecordEntity() {
    }

    public static IdempotencyRecordEntity inProgress(UUID userId, String requestPath,
                                                      String idempotencyKey, String requestHash,
                                                      Instant expiresAt) {
        if (userId == null || requestPath == null || requestPath.isBlank()
                || idempotencyKey == null || idempotencyKey.isBlank()
                || requestHash == null || requestHash.isBlank() || expiresAt == null) {
            throw new IllegalArgumentException("idempotency metadata is invalid");
        }
        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.id = UUID.randomUUID();
        record.userId = userId;
        record.requestPath = requestPath;
        record.idempotencyKey = idempotencyKey;
        record.requestHash = requestHash;
        record.expiresAt = expiresAt;
        return record;
    }

    public void complete(UUID userMessageId, UUID assistantMessageId, int responseHttpStatus,
                         String responseStatus) {
        if (userMessageId == null || assistantMessageId == null || responseHttpStatus < 100
                || responseStatus == null || responseStatus.isBlank()) {
            throw new IllegalArgumentException("idempotency response is invalid");
        }
        ObjectMapper mapper = new ObjectMapper();
        var response = mapper.createObjectNode();
        response.put("userMessageId", userMessageId.toString());
        response.put("assistantMessageId", assistantMessageId.toString());
        response.put("status", responseStatus);
        this.responseHttpStatus = responseHttpStatus;
        this.responseBody = response;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getStatus() {
        return responseBody == null ? "IN_PROGRESS" : "COMPLETED";
    }

    public UUID getResponseUserMessageId() {
        return uuid("userMessageId");
    }

    public UUID getResponseAssistantMessageId() {
        return uuid("assistantMessageId");
    }

    public String getResponseStatus() {
        return responseBody == null ? null : responseBody.path("status").asText(null);
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    private UUID uuid(String field) {
        String value = responseBody == null ? null : responseBody.path(field).asText(null);
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
