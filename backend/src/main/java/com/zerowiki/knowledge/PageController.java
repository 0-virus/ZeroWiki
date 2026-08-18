package com.zerowiki.knowledge;

import com.zerowiki.common.ApiResponse;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/libraries/{libraryId}/pages")
public class PageController {
    private final KnowledgeService knowledgeService;

    public PageController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    ApiResponse<List<KnowledgeDtos.PageResponse>> list(Authentication authentication,
                                                       @PathVariable UUID libraryId,
                                                       @RequestParam(required = false) String type,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String topic,
                                                       @RequestParam(name = "q", required = false) String query,
                                                       @RequestParam(defaultValue = "20") int limit) {
        return new ApiResponse<>(knowledgeService.pages(userId(authentication), libraryId, type, status,
                topic, query, limit));
    }

    @GetMapping("/{pageId}")
    ApiResponse<KnowledgeDtos.PageResponse> get(Authentication authentication,
                                                @PathVariable UUID libraryId,
                                                @PathVariable UUID pageId) {
        return new ApiResponse<>(knowledgeService.page(userId(authentication), libraryId, pageId));
    }

    @GetMapping("/by-slug/{slug}")
    ApiResponse<KnowledgeDtos.PageResponse> bySlug(Authentication authentication,
                                                    @PathVariable UUID libraryId,
                                                    @PathVariable String slug) {
        return new ApiResponse<>(knowledgeService.pageBySlug(userId(authentication), libraryId, slug));
    }

    @GetMapping("/{pageId}/versions")
    ApiResponse<List<KnowledgeDtos.PageVersionResponse>> versions(Authentication authentication,
                                                                   @PathVariable UUID libraryId,
                                                                   @PathVariable UUID pageId) {
        return new ApiResponse<>(knowledgeService.versions(userId(authentication), libraryId, pageId));
    }

    @GetMapping("/{pageId}/versions/{versionNo}")
    ApiResponse<KnowledgeDtos.PageVersionResponse> version(Authentication authentication,
                                                            @PathVariable UUID libraryId,
                                                            @PathVariable UUID pageId,
                                                            @PathVariable int versionNo) {
        return new ApiResponse<>(knowledgeService.version(userId(authentication), libraryId, pageId, versionNo));
    }

    @GetMapping("/{pageId}/diff")
    ApiResponse<KnowledgeDtos.DiffResponse> diff(Authentication authentication,
                                                  @PathVariable UUID libraryId,
                                                  @PathVariable UUID pageId,
                                                  @RequestParam(defaultValue = "1") int from,
                                                  @RequestParam(required = false) Integer to) {
        return new ApiResponse<>(knowledgeService.diff(userId(authentication), libraryId, pageId, from, to));
    }

    @PostMapping("/{pageId}/edit-proposals")
    ResponseEntity<ApiResponse<KnowledgeDtos.ChangeSetResponse>> editProposal(Authentication authentication,
                                                                                @PathVariable UUID libraryId,
                                                                                @PathVariable UUID pageId,
                                                                                @Valid @RequestBody KnowledgeDtos.EditProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(knowledgeService.editProposal(
                userId(authentication), libraryId, pageId, request)));
    }

    @PostMapping("/{pageId}/archive")
    ResponseEntity<ApiResponse<KnowledgeDtos.ChangeSetResponse>> archive(Authentication authentication,
                                                                          @PathVariable UUID libraryId,
                                                                          @PathVariable UUID pageId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(knowledgeService.archiveProposal(
                userId(authentication), libraryId, pageId)));
    }

    @GetMapping("/{pageId}/claims")
    ApiResponse<List<KnowledgeDtos.ClaimResponse>> claims(Authentication authentication,
                                                          @PathVariable UUID libraryId,
                                                          @PathVariable UUID pageId,
                                                          @RequestParam(required = false) String include) {
        return new ApiResponse<>(knowledgeService.claims(userId(authentication), libraryId, pageId,
                "evidences".equalsIgnoreCase(include)));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
