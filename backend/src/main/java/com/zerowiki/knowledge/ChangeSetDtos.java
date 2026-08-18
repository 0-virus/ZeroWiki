package com.zerowiki.knowledge;

import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ChangeSetDtos {
    private ChangeSetDtos() {
    }

    public record ChangeSetResponse(UUID id, UUID libraryId, String title, String status, String riskLevel,
                                    String summary, Long baseLibraryVersionNo, Counts counts, long revision,
                                    Instant createdAt, Instant reviewedAt, Instant appliedAt) {
    }

    public record ChangeSetDetailResponse(UUID id, UUID libraryId, String title, String status, String riskLevel,
                                          String summary, Long baseLibraryVersionNo, Counts counts, long revision,
                                          Instant createdAt, Instant reviewedAt, Instant appliedAt,
                                          List<ChangeItemResponse> items) {
    }

    public record Counts(long total, long minor, long major) {
    }

    public record ChangeItemResponse(UUID id, UUID changeSetId, String targetType, UUID targetId,
                                     String operation, String riskLevel, String reason, JsonNode beforeSnapshot,
                                     JsonNode afterSnapshot, JsonNode evidenceSummary, BigDecimal aiConfidence,
                                     String reviewStatus, Instant createdAt) {
    }

    public record ReviewRequest(@NotNull Long expectedRevision,
                                @NotEmpty List<@Valid ReviewDecision> decisions) {
    }

    public record ReviewDecision(@NotNull UUID changeItemId, @NotNull String decision, String comment) {
    }

    public record ApplyRequest(@NotNull Long expectedChangeSetRevision,
                               @NotNull Long expectedLibraryVersion, String summary) {
    }

    public record ApplyResponse(UUID changeSetId, String status, LibraryVersionResponse libraryVersion,
                                UUID postIngestLintRunId) {
    }

    public record RecalculateRequest(Long expectedRevision) {
    }

    public record LibraryVersionResponse(UUID id, UUID libraryId, long versionNo, UUID changeSetId,
                                         String summary, UUID createdBy, Instant createdAt) {
    }

    public record VersionDiffResponse(LibraryVersionResponse from, LibraryVersionResponse to,
                                      List<ChangeItemResponse> changes) {
    }
}
