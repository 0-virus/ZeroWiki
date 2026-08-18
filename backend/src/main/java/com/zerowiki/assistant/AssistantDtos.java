package com.zerowiki.assistant;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AssistantDtos {
    private AssistantDtos() {
    }

    public record CreateConversationRequest(String title) {
    }

    public record ConversationResponse(UUID id, UUID libraryId, String title, String status,
                                       Instant createdAt, Instant updatedAt) {
        static ConversationResponse from(ConversationEntity value) {
            return new ConversationResponse(value.getId(), value.getLibraryId(), value.getTitle(),
                    value.getStatus(), value.getCreatedAt(), value.getUpdatedAt());
        }
    }

    public record ConversationDetailResponse(ConversationResponse conversation,
                                             List<MessageResponse> messages) {
    }

    public record SearchScope(Boolean currentLibrary, List<UUID> referencedLibraryIds) {
    }

    public record AskRequest(String content, String answerMode, String processingMode,
                             SearchScope searchScope) {
    }

    public record QueuedResponse(UUID userMessageId, UUID assistantMessageId, String status) {
    }

    public record Citation(UUID id, int order, UUID pageId, String pageTitle, UUID claimId,
                           UUID sourceVersionId, JsonNode locator) {
    }

    public record Suggestions(List<String> missingEvidence, boolean expandToReferencedLibraries,
                              boolean webSearchAvailable) {
    }

    public record MessageResponse(UUID id, UUID conversationId, String role, String status,
                                  String answerMode, String content, boolean insufficientEvidence,
                                  List<Citation> citations, Suggestions suggestions, Instant completedAt) {
    }

    public record SaveProposalTarget(String type, String pageType, String title) {
    }

    public record SaveProposalRequest(SaveProposalTarget target, List<UUID> includeCitationIds) {
    }

    public record SaveProposalResponse(UUID changeSetId, List<UUID> affectedPageIds) {
    }

    public record FeedbackRequest(String rating, String comment) {
    }
}
