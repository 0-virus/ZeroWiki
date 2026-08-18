package com.zerowiki.assistant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_citations")
public class MessageCitationEntity {
    @Id
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "page_version_id")
    private UUID pageVersionId;

    @Column(name = "claim_id")
    private UUID claimId;

    @Column(name = "evidence_id")
    private UUID evidenceId;

    @Column(name = "source_version_id")
    private UUID sourceVersionId;

    @Column(name = "citation_order", nullable = false)
    private int citationOrder;

    @Column(name = "quoted_text", columnDefinition = "text")
    private String quotedText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MessageCitationEntity() {
    }

    public static MessageCitationEntity create(UUID messageId, UUID pageVersionId, UUID claimId,
                                                UUID evidenceId, UUID sourceVersionId, int citationOrder,
                                                String quotedText) {
        if (messageId == null || citationOrder < 1
                || (pageVersionId == null && claimId == null && evidenceId == null && sourceVersionId == null)) {
            throw new IllegalArgumentException("citation metadata is invalid");
        }
        MessageCitationEntity citation = new MessageCitationEntity();
        citation.id = UUID.randomUUID();
        citation.messageId = messageId;
        citation.pageVersionId = pageVersionId;
        citation.claimId = claimId;
        citation.evidenceId = evidenceId;
        citation.sourceVersionId = sourceVersionId;
        citation.citationOrder = citationOrder;
        citation.quotedText = quotedText;
        return citation;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getPageVersionId() {
        return pageVersionId;
    }

    public UUID getClaimId() {
        return claimId;
    }

    public UUID getEvidenceId() {
        return evidenceId;
    }

    public UUID getSourceVersionId() {
        return sourceVersionId;
    }

    public int getCitationOrder() {
        return citationOrder;
    }

    public String getQuotedText() {
        return quotedText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
