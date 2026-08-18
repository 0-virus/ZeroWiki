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
@RequestMapping("/libraries/{libraryId}")
public class ChangeSetController {
    private final ChangeSetService changeSetService;

    public ChangeSetController(ChangeSetService changeSetService) {
        this.changeSetService = changeSetService;
    }

    @GetMapping("/change-sets")
    ApiResponse<List<ChangeSetDtos.ChangeSetResponse>> list(Authentication authentication,
                                                              @PathVariable UUID libraryId,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) String riskLevel) {
        return new ApiResponse<>(changeSetService.list(userId(authentication), libraryId, status, riskLevel));
    }

    @GetMapping("/change-sets/{changeSetId}")
    ApiResponse<ChangeSetDtos.ChangeSetDetailResponse> get(Authentication authentication,
                                                            @PathVariable UUID libraryId,
                                                            @PathVariable UUID changeSetId) {
        return new ApiResponse<>(changeSetService.get(userId(authentication), libraryId, changeSetId));
    }

    @GetMapping("/change-sets/{changeSetId}/items")
    ApiResponse<List<ChangeSetDtos.ChangeItemResponse>> items(Authentication authentication,
                                                               @PathVariable UUID libraryId,
                                                               @PathVariable UUID changeSetId) {
        return new ApiResponse<>(changeSetService.items(userId(authentication), libraryId, changeSetId));
    }

    @PostMapping("/change-sets/{changeSetId}/reviews")
    ResponseEntity<ApiResponse<ChangeSetDtos.ChangeSetDetailResponse>> review(Authentication authentication,
                                                                                @PathVariable UUID libraryId,
                                                                                @PathVariable UUID changeSetId,
                                                                                @Valid @RequestBody ChangeSetDtos.ReviewRequest request) {
        ChangeSetService.ReviewResult result = changeSetService.review(userId(authentication), libraryId,
                changeSetId, request);
        HttpStatus status = result.accepted() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(new ApiResponse<>(result.response()));
    }

    @PostMapping("/change-sets/{changeSetId}/apply")
    ResponseEntity<ApiResponse<ChangeSetDtos.ApplyResponse>> apply(Authentication authentication,
                                                                    @PathVariable UUID libraryId,
                                                                    @PathVariable UUID changeSetId,
                                                                    @Valid @RequestBody ChangeSetDtos.ApplyRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse<>(changeSetService.apply(
                userId(authentication), libraryId, changeSetId, request)));
    }

    @PostMapping("/change-sets/{changeSetId}/recalculate")
    ResponseEntity<ApiResponse<ChangeSetDtos.ChangeSetDetailResponse>> recalculate(Authentication authentication,
                                                                                    @PathVariable UUID libraryId,
                                                                                    @PathVariable UUID changeSetId,
                                                                                    @RequestBody(required = false)
                                                                                    ChangeSetDtos.RecalculateRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse<>(changeSetService.recalculate(
                userId(authentication), libraryId, changeSetId, request)));
    }

    @GetMapping("/versions")
    ApiResponse<List<ChangeSetDtos.LibraryVersionResponse>> versions(Authentication authentication,
                                                                      @PathVariable UUID libraryId) {
        return new ApiResponse<>(changeSetService.versions(userId(authentication), libraryId));
    }

    @GetMapping("/versions/diff")
    ApiResponse<ChangeSetDtos.VersionDiffResponse> diff(Authentication authentication,
                                                         @PathVariable UUID libraryId,
                                                         @RequestParam long from,
                                                         @RequestParam long to) {
        return new ApiResponse<>(changeSetService.diff(userId(authentication), libraryId, from, to));
    }

    @GetMapping("/versions/{versionNo}")
    ApiResponse<ChangeSetDtos.LibraryVersionResponse> version(Authentication authentication,
                                                               @PathVariable UUID libraryId,
                                                               @PathVariable long versionNo) {
        return new ApiResponse<>(changeSetService.version(userId(authentication), libraryId, versionNo));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
