package com.zerowiki.ingest;

import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class IngestDtos {
    private IngestDtos() {
    }

    public record CreateIngestRequest(
            @NotNull ImportType importType,
            @NotNull ProcessingMode processingMode,
            @NotEmpty List<UUID> sourceVersionIds) {
    }

    public enum PlanDecision {
        APPROVE,
        REJECT
    }

    public record PlanApprovalRequest(
            @NotNull PlanDecision decision,
            List<UUID> includedItemIds,
            ProcessingMode processingMode,
            Long expectedEstimatedTokens,
            Long expectedPlanRevision) {
    }

    public record AnswerRequest(@NotNull QuestionStatus status, String answer) {
    }

    public record BulkAnswerRequest(@NotEmpty @Valid List<AnswerEntry> answers) {
    }

    public record AnswerEntry(@NotNull UUID questionId, @NotBlank String answer) {
    }

    public record IngestResponse(UUID id, UUID libraryId, ImportType importType, ProcessingMode processingMode,
                                 IngestStatus status, Progress progress, PlanResponse plan, String nextAction,
                                 long planRevision, Instant createdAt, Instant updatedAt) {
        public static IngestResponse from(IngestJobEntity job) {
            return new IngestResponse(job.getId(), job.getLibraryId(), job.getImportType(), job.getProcessingMode(),
                    job.getStatus(), Progress.from(job), PlanResponse.from(job.getPlan()),
                    nextAction(job.getStatus()), job.getPlanRevision(), job.getCreatedAt(), job.getUpdatedAt());
        }

        private static String nextAction(IngestStatus status) {
            return switch (status) {
                case PLAN_REVIEW -> "APPROVE_PLAN";
                case QUESTION_REVIEW -> "ANSWER_QUESTIONS";
                case CHANGE_REVIEW -> "REVIEW_CHANGES";
                case FAILED -> "RETRY_FAILED_ITEMS";
                case PAUSED_QUOTA -> "PURCHASE_QUOTA";
                default -> null;
            };
        }
    }

    public record Progress(int completed, int total, int percent) {
        static Progress from(IngestJobEntity job) {
            int total = job.getProgressTotal();
            int percent = total == 0 ? 0 : (int) Math.floor((job.getProgressCompleted() * 100.0) / total);
            return new Progress(job.getProgressCompleted(), total, percent);
        }
    }

    public record PlanResponse(int documents, int processable, int duplicates, int heldForReview,
                               int topicGroups, long estimatedTokens, long estimatedBillableUnits) {
        static PlanResponse from(JsonNode plan) {
            if (plan == null || plan.isNull()) {
                return new PlanResponse(0, 0, 0, 0, 0, 0, 0);
            }
            return new PlanResponse(plan.path("documents").asInt(), plan.path("processable").asInt(),
                    plan.path("duplicates").asInt(), plan.path("heldForReview").asInt(),
                    plan.path("topicGroups").asInt(), plan.path("estimatedTokens").asLong(),
                    plan.path("estimatedBillableUnits").asLong());
        }
    }

    public record IngestItemResponse(UUID id, UUID ingestJobId, UUID sourceVersionId,
                                     IngestItemStatus status, int retryCount, String failureCode,
                                     String failureMessage, Instant createdAt, Instant updatedAt) {
        static IngestItemResponse from(IngestItemEntity item) {
            return new IngestItemResponse(item.getId(), item.getIngestJobId(), item.getSourceVersionId(),
                    item.getStatus(), item.getRetryCount(), item.getFailureCode(), item.getFailureMessage(),
                    item.getCreatedAt(), item.getUpdatedAt());
        }
    }

    public record QuestionResponse(UUID id, UUID ingestJobId, UUID ingestItemId, String question,
                                   String answer, QuestionStatus status, Instant createdAt, Instant answeredAt) {
        static QuestionResponse from(JobQuestionEntity question) {
            return new QuestionResponse(question.getId(), question.getIngestJobId(), question.getIngestItemId(),
                    question.getQuestion(), question.getAnswer(), question.getStatus(), question.getCreatedAt(),
                    question.getAnsweredAt());
        }
    }
}
