package com.zerowiki.knowledge;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class KnowledgeDtos {
    private KnowledgeDtos() {
    }

    public record PageResponse(UUID id, UUID libraryId, String slug, String pageType, String status,
                               JsonNode topics, PageVersionResponse version, ClaimSummary claimSummary,
                               List<RelatedPageResponse> relatedPages) {
    }

    public record PageVersionResponse(UUID id, UUID pageId, int versionNo, String title, String markdownBody,
                                      String summary, Instant createdAt) {
    }

    public record PageVersionSnapshot(int versionNo, String title, String markdownBody) {
    }

    public record DiffResponse(PageVersionSnapshot from, PageVersionSnapshot to) {
    }

    public record ClaimSummary(long total, long withoutEvidence, long openContradictions) {
    }

    public record RelatedPageResponse(UUID id, String slug, String title, String relationType) {
    }

    public record ClaimResponse(UUID id, UUID pageId, String statement, String knowledgeStatus,
                                BigDecimal confidence, JsonNode conditions, int evidenceCount,
                                boolean evidenceTruncated, List<EvidenceResponse> evidences) {
    }

    public record EvidenceResponse(UUID id, UUID claimId, String evidenceType, UUID sourceVersionId,
                                   UUID sourceId, String sourceTitle, JsonNode locator, String excerpt,
                                   String excerptLanguage) {
    }

    public record RelationResponse(UUID id, UUID libraryId, UUID sourcePageId, UUID targetPageId,
                                   String relationType, String rationale, BigDecimal confidence,
                                   String status, String createdByType, Instant createdAt) {
    }

    public record ContradictionResponse(UUID id, UUID libraryId, UUID leftClaimId, UUID rightClaimId,
                                        String leftClaimStatement, String rightClaimStatement,
                                        String classification, String explanation, BigDecimal confidence,
                                        String status, String resolutionNote, Instant resolvedAt,
                                        Instant createdAt) {
    }

    public record ChangeSetResponse(UUID changeSetId, String status, String riskLevel) {
    }

    public record EditProposalRequest(Integer baseVersionNo, @NotBlank String proposedMarkdown,
                                      @NotBlank String reason) {
    }

    public record RelationFeedbackRequest(String feedbackType, String comment, String scope) {
    }

    public record ResolutionRequest(String decision, String classification, String resolutionNote) {
    }
}
