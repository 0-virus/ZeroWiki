package com.zerowiki.assistant;

import com.zerowiki.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/libraries/{libraryId}")
public class AssistantController {
    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/conversations")
    ResponseEntity<ApiResponse<AssistantDtos.ConversationResponse>> createConversation(
            Authentication authentication, @PathVariable UUID libraryId,
            @RequestBody(required = false) AssistantDtos.CreateConversationRequest request) {
        ConversationEntity conversation = assistantService.createConversation(userId(authentication), libraryId,
                request == null ? null : request.title());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(AssistantDtos.ConversationResponse.from(conversation)));
    }

    @GetMapping("/conversations")
    ApiResponse<List<AssistantDtos.ConversationResponse>> conversations(Authentication authentication,
                                                                        @PathVariable UUID libraryId) {
        return new ApiResponse<>(assistantService.conversations(userId(authentication), libraryId));
    }

    @GetMapping("/conversations/{conversationId}")
    ApiResponse<AssistantDtos.ConversationDetailResponse> conversation(Authentication authentication,
                                                                        @PathVariable UUID libraryId,
                                                                        @PathVariable UUID conversationId) {
        return new ApiResponse<>(assistantService.conversation(userId(authentication), libraryId, conversationId));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    ResponseEntity<ApiResponse<AssistantDtos.QueuedResponse>> ask(Authentication authentication,
                                                                  @PathVariable UUID libraryId,
                                                                  @PathVariable UUID conversationId,
                                                                  @RequestBody AssistantDtos.AskRequest request,
                                                                  @RequestHeader(name = "Idempotency-Key", required = false)
                                                                  String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse<>(assistantService.ask(
                userId(authentication), libraryId, conversationId, request, idempotencyKey)));
    }

    @GetMapping("/conversations/{conversationId}/messages/{messageId}")
    ApiResponse<AssistantDtos.MessageResponse> message(Authentication authentication,
                                                       @PathVariable UUID libraryId,
                                                       @PathVariable UUID conversationId,
                                                       @PathVariable UUID messageId) {
        return new ApiResponse<>(assistantService.message(userId(authentication), libraryId, conversationId, messageId));
    }

    @PostMapping("/messages/{messageId}/save-proposal")
    ResponseEntity<ApiResponse<AssistantDtos.SaveProposalResponse>> saveProposal(Authentication authentication,
                                                                                  @PathVariable UUID libraryId,
                                                                                  @PathVariable UUID messageId,
                                                                                  @RequestBody AssistantDtos.SaveProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(assistantService.saveProposal(
                userId(authentication), libraryId, messageId, request)));
    }

    @PostMapping("/messages/{messageId}/feedback")
    ResponseEntity<ApiResponse<Object>> feedback(Authentication authentication,
                                                 @PathVariable UUID libraryId,
                                                 @PathVariable UUID messageId,
                                                 @RequestBody(required = false) AssistantDtos.FeedbackRequest request) {
        // ASSUMPTION(미확정 API §18): feedback wire contract은 rating/comment 최소 형태로만 저장한다.
        if (!assistantService.feedback(userId(authentication), libraryId, messageId, request)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new ApiResponse<>(java.util.Map.of("accepted", true)));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
