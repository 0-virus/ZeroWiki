package com.zerowiki.search;

import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

public final class SearchDtos {
    private SearchDtos() {
    }

    public record Origin(String scope, UUID libraryId, String libraryName) {
    }

    public record SearchResult(String resultType, String title, String snippet, double score,
                               UUID pageId, UUID pageVersionId, UUID sourceId, UUID claimId,
                               UUID sourceVersionId, JsonNode locator, Origin origin) {
    }

    public record SearchResponse(List<SearchResult> results, String mode) {
    }

    public record SearchExpansionRequest(String query, List<UUID> targetLibraryIds,
                                         String mode, Integer limit) {
    }

    public record SearchExpansionResponse(List<SearchResult> results, String mode,
                                          List<UUID> expandedLibraryIds) {
    }
}
