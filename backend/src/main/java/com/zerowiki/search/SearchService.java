package com.zerowiki.search;

import com.zerowiki.common.ApiException;
import com.zerowiki.knowledge.ClaimEntity;
import com.zerowiki.knowledge.ClaimRepository;
import com.zerowiki.knowledge.EvidenceEntity;
import com.zerowiki.knowledge.EvidenceRepository;
import com.zerowiki.knowledge.PageEntity;
import com.zerowiki.knowledge.PageRepository;
import com.zerowiki.knowledge.PageVersionEntity;
import com.zerowiki.knowledge.PageVersionRepository;
import com.zerowiki.library.LibraryEntity;
import com.zerowiki.library.LibraryReferenceRepository;
import com.zerowiki.library.LibraryRepository;
import com.zerowiki.source.LibrarySourceEntity;
import com.zerowiki.source.LibrarySourceRepository;
import com.zerowiki.source.SourceEntity;
import com.zerowiki.source.SourceRepository;
import com.zerowiki.source.SourceVersionEntity;
import com.zerowiki.source.SourceVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SearchService {
    private static final Set<String> MODES = Set.of("KEYWORD", "SEMANTIC", "HYBRID");
    private static final Set<String> TYPES = Set.of("PAGE", "CLAIM", "SOURCE");
    private static final Pattern TOKEN_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final LibraryRepository libraryRepository;
    private final LibraryReferenceRepository referenceRepository;
    private final LibrarySourceRepository librarySourceRepository;
    private final SourceRepository sourceRepository;
    private final SourceVersionRepository sourceVersionRepository;
    private final PageRepository pageRepository;
    private final PageVersionRepository pageVersionRepository;
    private final ClaimRepository claimRepository;
    private final EvidenceRepository evidenceRepository;

    public SearchService(LibraryRepository libraryRepository,
                         LibraryReferenceRepository referenceRepository,
                         LibrarySourceRepository librarySourceRepository,
                         SourceRepository sourceRepository,
                         SourceVersionRepository sourceVersionRepository,
                         PageRepository pageRepository,
                         PageVersionRepository pageVersionRepository,
                         ClaimRepository claimRepository,
                         EvidenceRepository evidenceRepository) {
        this.libraryRepository = libraryRepository;
        this.referenceRepository = referenceRepository;
        this.librarySourceRepository = librarySourceRepository;
        this.sourceRepository = sourceRepository;
        this.sourceVersionRepository = sourceVersionRepository;
        this.pageRepository = pageRepository;
        this.pageVersionRepository = pageVersionRepository;
        this.claimRepository = claimRepository;
        this.evidenceRepository = evidenceRepository;
    }

    @Transactional(readOnly = true)
    public SearchDtos.SearchResponse search(UUID ownerId, UUID libraryId, String query,
                                            String types, String mode, Integer limit) {
        LibraryEntity library = requireLibrary(ownerId, libraryId);
        Set<String> requestedTypes = parseTypes(types);
        String effectiveMode = normalizeMode(mode);
        return new SearchDtos.SearchResponse(find(ownerId, library, query, requestedTypes,
                effectiveMode, "CURRENT", limitOrDefault(limit)), effectiveMode);
    }

    @Transactional(readOnly = true)
    public SearchDtos.SearchExpansionResponse expand(UUID ownerId, UUID currentLibraryId,
                                                     SearchDtos.SearchExpansionRequest request) {
        requireLibrary(ownerId, currentLibraryId);
        if (request == null || request.targetLibraryIds() == null || request.targetLibraryIds().isEmpty()) {
            throw badRequest("targetLibraryIds must not be empty");
        }
        String effectiveMode = normalizeMode(request.mode());
        int limit = limitOrDefault(request.limit());
        List<SearchDtos.SearchResult> results = new ArrayList<>();
        Set<UUID> expanded = new LinkedHashSet<>();
        for (UUID targetId : request.targetLibraryIds()) {
            if (targetId == null || targetId.equals(currentLibraryId)) {
                throw boundary("referenced library must be distinct from current library");
            }
            if (referenceRepository.findBySourceLibraryIdAndTargetLibraryId(currentLibraryId, targetId).isEmpty()) {
                throw boundary("library reference is not registered");
            }
            LibraryEntity target = requireLibrary(ownerId, targetId);
            expanded.add(targetId);
            results.addAll(find(ownerId, target, request.query(), Set.of("PAGE", "CLAIM", "SOURCE"),
                    effectiveMode, "REFERENCED", limit));
        }
        return new SearchDtos.SearchExpansionResponse(results.stream().limit(limit).toList(), effectiveMode,
                expanded.stream().toList());
    }

    /**
     * Phase 1 keeps the semantic surface stable while using deterministic in-memory keyword scoring.
     * // ponytail: replace this fallback only after a measured pgvector provider is approved.
     */
    @Transactional(readOnly = true)
    public List<SearchDtos.SearchResult> results(UUID ownerId, UUID libraryId, String query,
                                                 Set<String> types, String mode, int limit,
                                                 String scope) {
        LibraryEntity library = requireLibrary(ownerId, libraryId);
        return find(ownerId, library, query, types, normalizeMode(mode), scope, limit);
    }

    @Transactional(readOnly = true)
    public List<SearchDtos.SearchResult> referencedResults(UUID ownerId, UUID currentLibraryId,
                                                           UUID targetLibraryId, String query,
                                                           Set<String> types, String mode, int limit) {
        requireLibrary(ownerId, currentLibraryId);
        if (targetLibraryId == null || targetLibraryId.equals(currentLibraryId)
                || referenceRepository.findBySourceLibraryIdAndTargetLibraryId(currentLibraryId, targetLibraryId).isEmpty()) {
            throw boundary("library reference is not registered");
        }
        LibraryEntity target = requireLibrary(ownerId, targetLibraryId);
        return find(ownerId, target, query, types, normalizeMode(mode), "REFERENCED", limit);
    }

    private List<SearchDtos.SearchResult> find(UUID ownerId, LibraryEntity library, String query,
                                               Set<String> types, String mode, String scope, int limit) {
        String normalizedQuery = query == null ? "" : query.trim();
        Set<String> requestedTypes = types == null || types.isEmpty() ? Set.of("PAGE", "CLAIM", "SOURCE") : types;
        List<String> tokens = tokens(normalizedQuery);
        // ponytail: Phase 1 scans the owner-scoped published set in memory so OR scoring is not
        // narrowed by PostgreSQL plainto_tsquery's AND semantics; add a measured prefilter only
        // when the query/index contract preserves this candidate set.
        SearchDtos.Origin origin = new SearchDtos.Origin(scope, library.getId(), library.getName());
        List<ScoredResult> candidates = new ArrayList<>();

        if (requestedTypes.contains("PAGE") || requestedTypes.contains("CLAIM")) {
            for (PageEntity page : pageRepository.findAllByLibraryIdOrderByUpdatedAtDesc(library.getId())) {
                if (!"PUBLISHED".equals(page.getStatus())) {
                    continue;
                }
                PageVersionEntity version = pageVersionRepository.findTopByPageIdOrderByVersionNoDesc(page.getId())
                        .orElse(null);
                if (version == null) {
                    continue;
                }
                if (requestedTypes.contains("PAGE")) {
                    add(candidates, "PAGE", version.getTitle(), version.getMarkdownBody(), page.getId(),
                            version.getId(), null, null, null, null, origin, tokens);
                }
                if (requestedTypes.contains("CLAIM")) {
                    for (ClaimEntity claim : claimRepository.findAllByPageIdOrderByCreatedAtAsc(page.getId())) {
                        EvidenceEntity evidence = evidenceRepository.findAllByClaimIdOrderByCreatedAtAsc(claim.getId())
                                .stream().findFirst().orElse(null);
                        SourceVersionEntity sourceVersion = evidence == null ? null
                                : sourceVersionRepository.findById(evidence.getSourceVersionId()).orElse(null);
                        if (sourceVersion != null && sourceRepository
                                .findByIdAndOwnerId(sourceVersion.getSourceId(), ownerId).isEmpty()) {
                            sourceVersion = null;
                        }
                        add(candidates, "CLAIM", claim.getStatement(), claim.getStatement(), page.getId(),
                                version.getId(), null, claim.getId(), sourceVersion == null ? null : sourceVersion.getId(),
                                evidence == null ? null : evidence.getLocator(), origin, tokens);
                    }
                }
            }
        }

        if (requestedTypes.contains("SOURCE")) {
            for (LibrarySourceEntity linked : librarySourceRepository
                    .findAllByIdLibraryIdOrderByLinkedAtDesc(library.getId())) {
                SourceEntity source = sourceRepository.findByIdAndOwnerId(linked.getSourceId(), ownerId).orElse(null);
                if (source == null) {
                    continue;
                }
                SourceVersionEntity version = sourceVersionRepository.findTopBySourceIdOrderByVersionNoDesc(source.getId())
                        .orElse(null);
                String snippet = version == null ? source.getCanonicalUri() : source.getCanonicalUri();
                add(candidates, "SOURCE", source.getTitle(), snippet, null, null, source.getId(), null,
                        version == null ? null : version.getId(), null, origin, tokens);
            }
        }

        return candidates.stream()
                .sorted(Comparator.comparingDouble(ScoredResult::score).reversed()
                        .thenComparing(value -> value.result().title(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(value -> value.result().resultType()))
                .limit(Math.max(1, Math.min(limit, 100)))
                .map(ScoredResult::result)
                .toList();
    }

    private void add(List<ScoredResult> candidates, String resultType, String title, String text,
                     UUID pageId, UUID pageVersionId, UUID sourceId, UUID claimId, UUID sourceVersionId,
                     tools.jackson.databind.JsonNode locator, SearchDtos.Origin origin, List<String> tokens) {
        String searchable = ((title == null ? "" : title) + " " + (text == null ? "" : text)).toLowerCase(Locale.ROOT);
        double score = score(searchable, tokens);
        if (score <= 0) {
            return;
        }
        candidates.add(new ScoredResult(new SearchDtos.SearchResult(resultType, title, snippet(text), score,
                pageId, pageVersionId, sourceId, claimId, sourceVersionId,
                locator == null ? null : locator.deepCopy(), origin), score));
    }

    private double score(String searchable, List<String> tokens) {
        if (tokens.isEmpty()) {
            return 1.0;
        }
        long matched = tokens.stream().filter(searchable::contains).count();
        return matched == 0 ? 0 : (double) matched / tokens.size();
    }

    private String snippet(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() <= 400 ? compact : compact.substring(0, 400);
    }

    private List<String> tokens(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return Arrays.stream(TOKEN_SEPARATOR.split(query.toLowerCase(Locale.ROOT)))
                .map(this::stripKoreanParticle)
                .filter(token -> token.length() > 1)
                .distinct()
                .toList();
    }

    private String stripKoreanParticle(String token) {
        for (int index = 1; index < token.length(); index++) {
            char value = token.charAt(index);
            if (value >= '\uAC00' && value <= '\uD7A3') {
                String latinPrefix = token.substring(0, index);
                return latinPrefix.length() > 1 ? latinPrefix : token;
            }
        }
        return token;
    }

    private Set<String> parseTypes(String value) {
        if (value == null || value.isBlank()) {
            return Set.of("PAGE", "CLAIM", "SOURCE");
        }
        Set<String> parsed = new HashSet<>();
        for (String item : value.split(",")) {
            String type = item.trim().toUpperCase(Locale.ROOT);
            if (!TYPES.contains(type)) {
                throw badRequest("unsupported search result type");
            }
            parsed.add(type);
        }
        return parsed;
    }

    private String normalizeMode(String mode) {
        String normalized = mode == null || mode.isBlank() ? "KEYWORD" : mode.trim().toUpperCase(Locale.ROOT);
        if (!MODES.contains(normalized)) {
            throw badRequest("unsupported search mode");
        }
        return normalized;
    }

    private int limitOrDefault(Integer limit) {
        return limit == null ? 20 : Math.max(1, Math.min(limit, 100));
    }

    private LibraryEntity requireLibrary(UUID ownerId, UUID libraryId) {
        return libraryRepository.findByIdAndOwnerId(libraryId, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "library not found"));
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SEARCH_REQUEST", message);
    }

    private ApiException boundary(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "REFERENCE_BOUNDARY", message);
    }

    private record ScoredResult(SearchDtos.SearchResult result, double score) {
    }
}
