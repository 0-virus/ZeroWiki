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
@RequestMapping("/libraries/{libraryId}/contradictions")
public class ContradictionController {
    private final KnowledgeService knowledgeService;

    public ContradictionController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    ApiResponse<List<KnowledgeDtos.ContradictionResponse>> list(Authentication authentication,
                                                                @PathVariable UUID libraryId,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(required = false) String classification,
                                                                @RequestParam(required = false) Instant since,
                                                                @RequestParam(defaultValue = "50") int limit) {
        return new ApiResponse<>(knowledgeService.contradictions(userId(authentication), libraryId, status,
                classification, since, limit));
    }

    @GetMapping("/{contradictionId}")
    ApiResponse<KnowledgeDtos.ContradictionResponse> get(Authentication authentication,
                                                         @PathVariable UUID libraryId,
                                                         @PathVariable UUID contradictionId) {
        return new ApiResponse<>(knowledgeService.contradiction(userId(authentication), libraryId, contradictionId));
    }

    @PostMapping("/{contradictionId}/resolution")
    ResponseEntity<ApiResponse<KnowledgeDtos.ChangeSetResponse>> resolution(Authentication authentication,
                                                                             @PathVariable UUID libraryId,
                                                                             @PathVariable UUID contradictionId,
                                                                             @RequestBody(required = false) KnowledgeDtos.ResolutionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(knowledgeService.resolutionProposal(
                userId(authentication), libraryId, contradictionId, request)));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
