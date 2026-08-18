package com.zerowiki.knowledge;

import com.zerowiki.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/libraries/{libraryId}/claims")
public class ClaimController {
    private final KnowledgeService knowledgeService;

    public ClaimController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/{claimId}")
    ApiResponse<KnowledgeDtos.ClaimResponse> get(Authentication authentication,
                                                 @PathVariable UUID libraryId,
                                                 @PathVariable UUID claimId) {
        return new ApiResponse<>(knowledgeService.claim(userId(authentication), libraryId, claimId));
    }

    @GetMapping("/{claimId}/evidences")
    ApiResponse<List<KnowledgeDtos.EvidenceResponse>> evidences(Authentication authentication,
                                                                 @PathVariable UUID libraryId,
                                                                 @PathVariable UUID claimId) {
        return new ApiResponse<>(knowledgeService.evidences(userId(authentication), libraryId, claimId));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
