package com.zerowiki.search;

import com.zerowiki.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/libraries/{libraryId}")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    ApiResponse<SearchDtos.SearchResponse> search(Authentication authentication,
                                                  @PathVariable UUID libraryId,
                                                  @RequestParam(name = "q", required = false) String query,
                                                  @RequestParam(required = false) String types,
                                                  @RequestParam(required = false) String mode,
                                                  @RequestParam(required = false) Integer limit) {
        return new ApiResponse<>(searchService.search(userId(authentication), libraryId, query, types, mode, limit));
    }

    @PostMapping("/search-expansions")
    ResponseEntity<ApiResponse<SearchDtos.SearchExpansionResponse>> expand(Authentication authentication,
                                                                            @PathVariable UUID libraryId,
                                                                            @Valid @RequestBody SearchDtos.SearchExpansionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(searchService.expand(userId(authentication), libraryId, request)));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
