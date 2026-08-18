package com.zerowiki.knowledge;

import com.zerowiki.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/libraries/{libraryId}/relations")
public class RelationController {
    private final KnowledgeService knowledgeService;

    public RelationController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    ApiResponse<List<KnowledgeDtos.RelationResponse>> list(Authentication authentication,
                                                           @PathVariable UUID libraryId,
                                                           @RequestParam(required = false) UUID pageId,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) Instant since,
                                                           @RequestParam(defaultValue = "50") int limit) {
        return new ApiResponse<>(knowledgeService.relations(userId(authentication), libraryId, pageId, status,
                since, limit));
    }

    @PostMapping("/{relationId}/feedback")
    ResponseEntity<ApiResponse<KnowledgeDtos.ChangeSetResponse>> feedback(Authentication authentication,
                                                                            @PathVariable UUID libraryId,
                                                                            @PathVariable UUID relationId,
                                                                            @RequestBody(required = false) KnowledgeDtos.RelationFeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(knowledgeService.relationFeedback(
                userId(authentication), libraryId, relationId, request)));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
