package com.zerowiki.knowledge;

import com.zerowiki.common.ApiException;
import com.zerowiki.library.LibraryEntity;
import com.zerowiki.library.LibraryRepository;
import com.zerowiki.source.SourceEntity;
import com.zerowiki.source.SourceRepository;
import com.zerowiki.source.SourceVersionEntity;
import com.zerowiki.source.SourceVersionRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Service
public class KnowledgeService {
    private final LibraryRepository libraryRepository;
    private final PageRepository pageRepository;
    private final PageVersionRepository pageVersionRepository;
    private final ClaimRepository claimRepository;
    private final ClaimStatusRepository claimStatusRepository;
    private final EvidenceRepository evidenceRepository;
    private final PageRelationRepository relationRepository;
    private final ContradictionRepository contradictionRepository;
    private final ChangeSetRepository changeSetRepository;
    private final ChangeItemRepository changeItemRepository;
    private final SourceRepository sourceRepository;
    private final SourceVersionRepository sourceVersionRepository;
    private final ObjectMapper objectMapper;

    public KnowledgeService(LibraryRepository libraryRepository,
                            PageRepository pageRepository,
                            PageVersionRepository pageVersionRepository,
                            ClaimRepository claimRepository,
                            ClaimStatusRepository claimStatusRepository,
                            EvidenceRepository evidenceRepository,
                            PageRelationRepository relationRepository,
                            ContradictionRepository contradictionRepository,
                            ChangeSetRepository changeSetRepository,
                            ChangeItemRepository changeItemRepository,
                            SourceRepository sourceRepository,
                            SourceVersionRepository sourceVersionRepository,
                            ObjectMapper objectMapper) {
        this.libraryRepository = libraryRepository;
        this.pageRepository = pageRepository;
        this.pageVersionRepository = pageVersionRepository;
        this.claimRepository = claimRepository;
        this.claimStatusRepository = claimStatusRepository;
        this.evidenceRepository = evidenceRepository;
        this.relationRepository = relationRepository;
        this.contradictionRepository = contradictionRepository;
        this.changeSetRepository = changeSetRepository;
        this.changeItemRepository = changeItemRepository;
        this.sourceRepository = sourceRepository;
        this.sourceVersionRepository = sourceVersionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDtos.PageResponse> pages(UUID ownerId, UUID libraryId, String type, String status,
                                                   String topic, String query, int limit) {
        requireLibrary(ownerId, libraryId);
        String effectiveStatus = status == null ? "PUBLISHED" : status;
        Predicate<PageEntity> matches = page -> (type == null || type.equalsIgnoreCase(page.getPageType()))
                && effectiveStatus.equalsIgnoreCase(page.getStatus())
                && (topic == null || containsTopic(page.getTopics(), topic))
                && (query == null || query.isBlank() || matchesQuery(page, query));
        return pageRepository.findAllByLibraryIdOrderByUpdatedAtDesc(libraryId).stream()
                .filter(matches)
                .limit(Math.max(1, Math.min(limit, 100)))
                .map(page -> pageView(ownerId, page, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDtos.PageResponse page(UUID ownerId, UUID libraryId, UUID pageId) {
        return pageView(ownerId, requirePage(ownerId, libraryId, pageId), true);
    }

    @Transactional(readOnly = true)
    public KnowledgeDtos.PageResponse pageBySlug(UUID ownerId, UUID libraryId, String slug) {
        requireLibrary(ownerId, libraryId);
        PageEntity page = pageRepository.findByLibraryIdAndSlug(libraryId, slug)
                .orElseThrow(() -> notFound("page not found"));
        return pageView(ownerId, page, true);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDtos.PageVersionResponse> versions(UUID ownerId, UUID libraryId, UUID pageId) {
        PageEntity page = requirePage(ownerId, libraryId, pageId);
        return pageVersionRepository.findAllByPageIdOrderByVersionNoDesc(page.getId()).stream()
                .map(KnowledgeService::versionView)
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDtos.PageVersionResponse version(UUID ownerId, UUID libraryId, UUID pageId, int versionNo) {
        PageEntity page = requirePage(ownerId, libraryId, pageId);
        return versionView(pageVersionRepository.findByPageIdAndVersionNo(page.getId(), versionNo)
                .orElseThrow(() -> notFound("page version not found")));
    }

    @Transactional(readOnly = true)
    public KnowledgeDtos.DiffResponse diff(UUID ownerId, UUID libraryId, UUID pageId, int from, Integer to) {
        PageEntity page = requirePage(ownerId, libraryId, pageId);
        int target = to == null ? latestVersionNo(page.getId()) : to;
        PageVersionEntity fromVersion = pageVersionRepository.findByPageIdAndVersionNo(page.getId(), from)
                .orElseThrow(() -> notFound("page version not found"));
        PageVersionEntity toVersion = pageVersionRepository.findByPageIdAndVersionNo(page.getId(), target)
                .orElseThrow(() -> notFound("page version not found"));
        return new KnowledgeDtos.DiffResponse(snapshot(fromVersion), snapshot(toVersion));
    }

    @Transactional
    public KnowledgeDtos.ChangeSetResponse editProposal(UUID ownerId, UUID libraryId, UUID pageId,
                                                         KnowledgeDtos.EditProposalRequest request) {
        PageEntity page = requirePage(ownerId, libraryId, pageId);
        PageVersionEntity base = pageVersionRepository.findByPageIdAndVersionNo(page.getId(), request.baseVersionNo())
                .orElseThrow(() -> notFound("base page version not found"));
        ObjectNode before = objectMapper.createObjectNode();
        before.put("versionNo", base.getVersionNo());
        before.put("markdownBody", base.getMarkdownBody());
        ObjectNode after = objectMapper.createObjectNode();
        after.put("baseVersionNo", base.getVersionNo());
        after.put("proposedMarkdown", request.proposedMarkdown());
        ChangeSetEntity changeSet = changeSetRepository.save(ChangeSetEntity.ready(
                libraryId, "EDIT_PROPOSAL", pageId, "Page edit proposal", request.reason(), "MINOR"));
        changeItemRepository.save(ChangeItemEntity.update(changeSet.getId(), "PAGE", pageId, before, after));
        return changeSetView(changeSet);
    }

    @Transactional
    public KnowledgeDtos.ChangeSetResponse archiveProposal(UUID ownerId, UUID libraryId, UUID pageId) {
        requirePage(ownerId, libraryId, pageId);
        ChangeSetEntity changeSet = changeSetRepository.save(ChangeSetEntity.ready(
                libraryId, "EDIT_PROPOSAL", pageId, "Page archive proposal", "Archive page", "MAJOR"));
        changeItemRepository.save(ChangeItemEntity.update(changeSet.getId(), "PAGE", pageId, null,
                objectMapper.createObjectNode().put("status", "ARCHIVED")));
        return changeSetView(changeSet);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDtos.ClaimResponse> claims(UUID ownerId, UUID libraryId, UUID pageId, boolean includeEvidences) {
        PageEntity page = requirePage(ownerId, libraryId, pageId);
        UUID versionId = currentVersion(page).map(PageVersionEntity::getId).orElse(null);
        return claimRepository.findAllByPageIdOrderByCreatedAtAsc(page.getId()).stream()
                .map(claim -> claimView(ownerId, claim, versionId, includeEvidences))
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDtos.ClaimResponse claim(UUID ownerId, UUID libraryId, UUID claimId) {
        ClaimEntity claim = requireClaim(ownerId, libraryId, claimId);
        PageEntity page = requirePage(ownerId, libraryId, claim.getPageId());
        UUID versionId = currentVersion(page).map(PageVersionEntity::getId).orElse(null);
        return claimView(ownerId, claim, versionId, true);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDtos.EvidenceResponse> evidences(UUID ownerId, UUID libraryId, UUID claimId) {
        ClaimEntity claim = requireClaim(ownerId, libraryId, claimId);
        return evidenceRepository.findAllByClaimIdOrderByCreatedAtAsc(claim.getId()).stream()
                .map(evidence -> evidenceView(ownerId, evidence))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDtos.RelationResponse> relations(UUID ownerId, UUID libraryId, UUID pageId,
                                                          String status, Instant since, int limit) {
        requireLibrary(ownerId, libraryId);
        List<PageRelationEntity> relations;
        if (status != null && since != null) {
            relations = relationRepository.findAllByLibraryIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                    libraryId, status, since);
        } else if (status != null) {
            relations = relationRepository.findAllByLibraryIdAndStatusOrderByCreatedAtDesc(libraryId, status);
        } else if (since != null) {
            relations = relationRepository.findAllByLibraryIdAndCreatedAtAfterOrderByCreatedAtDesc(libraryId, since);
        } else {
            relations = relationRepository.findAllByLibraryIdOrderByCreatedAtDesc(libraryId);
        }
        return relations.stream()
                .filter(relation -> pagesBelongToLibrary(libraryId, relation))
                .filter(relation -> pageId == null || pageId.equals(relation.getSourcePageId())
                        || pageId.equals(relation.getTargetPageId()))
                .limit(Math.max(1, Math.min(limit, 100)))
                .map(this::relationView)
                .toList();
    }

    @Transactional
    public KnowledgeDtos.ChangeSetResponse relationFeedback(UUID ownerId, UUID libraryId, UUID relationId,
                                                             KnowledgeDtos.RelationFeedbackRequest request) {
        requireLibrary(ownerId, libraryId);
        PageRelationEntity relation = relationRepository.findById(relationId)
                .filter(value -> libraryId.equals(value.getLibraryId()))
                .orElseThrow(() -> notFound("relation not found"));
        if (!pagesBelongToLibrary(libraryId, relation)) {
            throw notFound("relation not found");
        }
        String note = request == null ? "" : request.comment();
        ChangeSetEntity changeSet = changeSetRepository.save(ChangeSetEntity.ready(
                libraryId, "USER_EDIT", relation.getId(), "Relation feedback", note, "MINOR"));
        ObjectNode after = objectMapper.createObjectNode();
        if (request != null && request.feedbackType() != null) {
            after.put("feedbackType", request.feedbackType());
        }
        if (request != null && request.comment() != null) {
            after.put("comment", request.comment());
        }
        if (request != null && request.scope() != null) {
            after.put("scope", request.scope());
        }
        changeItemRepository.save(ChangeItemEntity.update(changeSet.getId(), "RELATION", relation.getId(), null, after));
        return changeSetView(changeSet);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDtos.ContradictionResponse> contradictions(UUID ownerId, UUID libraryId, String status,
                                                                     String classification, Instant since, int limit) {
        requireLibrary(ownerId, libraryId);
        String effectiveStatus = status == null ? "OPEN" : status;
        List<ContradictionEntity> contradictions;
        if (since != null) {
            contradictions = contradictionRepository
                    .findAllByLibraryIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(libraryId, effectiveStatus, since);
        } else {
            contradictions = contradictionRepository
                    .findAllByLibraryIdAndStatusOrderByCreatedAtDesc(libraryId, effectiveStatus);
        }
        return contradictions.stream()
                .filter(value -> classification == null || classification.equals(value.getClassification()))
                .filter(value -> claimsBelongToLibrary(libraryId, value))
                .limit(Math.max(1, Math.min(limit, 100)))
                .map(value -> contradictionView(libraryId, value))
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDtos.ContradictionResponse contradiction(UUID ownerId, UUID libraryId, UUID contradictionId) {
        requireLibrary(ownerId, libraryId);
        ContradictionEntity contradiction = contradictionRepository.findByIdAndLibraryId(contradictionId, libraryId)
                .orElseThrow(() -> notFound("contradiction not found"));
        if (!claimsBelongToLibrary(libraryId, contradiction)) {
            throw notFound("contradiction not found");
        }
        return contradictionView(libraryId, contradiction);
    }

    @Transactional
    public KnowledgeDtos.ChangeSetResponse resolutionProposal(UUID ownerId, UUID libraryId, UUID contradictionId,
                                                              KnowledgeDtos.ResolutionRequest request) {
        KnowledgeDtos.ContradictionResponse contradiction = contradiction(ownerId, libraryId, contradictionId);
        String note = request == null ? "" : request.resolutionNote();
        ChangeSetEntity changeSet = changeSetRepository.save(ChangeSetEntity.ready(
                libraryId, "USER_EDIT", contradictionId, "Contradiction resolution proposal", note, "MAJOR"));
        ObjectNode after = objectMapper.createObjectNode();
        if (request != null && request.decision() != null) {
            after.put("decision", request.decision());
        }
        if (request != null && request.classification() != null) {
            after.put("classification", request.classification());
        }
        if (request != null && request.resolutionNote() != null) {
            after.put("resolutionNote", request.resolutionNote());
        }
        changeItemRepository.save(ChangeItemEntity.update(changeSet.getId(), "CONTRADICTION", contradictionId, null, after));
        return changeSetView(changeSet);
    }

    private KnowledgeDtos.PageResponse pageView(UUID ownerId, PageEntity page, boolean detailed) {
        PageVersionEntity version = currentVersion(page).orElse(null);
        List<KnowledgeDtos.RelatedPageResponse> related = detailed
                ? relatedPages(ownerId, page)
                : List.of();
        List<ClaimEntity> claims = claimRepository.findAllByPageIdOrderByCreatedAtAsc(page.getId());
        long withoutEvidence = claims.stream()
                .filter(claim -> evidenceRepository.findAllByClaimIdOrderByCreatedAtAsc(claim.getId()).isEmpty())
                .count();
        Set<UUID> claimIds = claims.stream().map(ClaimEntity::getId).collect(java.util.stream.Collectors.toSet());
        long openContradictions = contradictionRepository
                .findAllByLibraryIdAndStatusOrderByCreatedAtDesc(page.getLibraryId(), "OPEN").stream()
                .filter(value -> claimIds.contains(value.getLeftClaimId()) || claimIds.contains(value.getRightClaimId()))
                .count();
        return new KnowledgeDtos.PageResponse(page.getId(), page.getLibraryId(), page.getSlug(), page.getPageType(),
                page.getStatus(), page.getTopics(), version == null ? null : versionView(version),
                new KnowledgeDtos.ClaimSummary(claims.size(), withoutEvidence, openContradictions), related);
    }

    private List<KnowledgeDtos.RelatedPageResponse> relatedPages(UUID ownerId, PageEntity page) {
        requireLibrary(ownerId, page.getLibraryId());
        return relationRepository.findAllByLibraryIdOrderByCreatedAtDesc(page.getLibraryId()).stream()
                .filter(relation -> page.getId().equals(relation.getSourcePageId())
                        || page.getId().equals(relation.getTargetPageId()))
                .filter(relation -> pagesBelongToLibrary(page.getLibraryId(), relation))
                .map(relation -> {
                    UUID relatedId = page.getId().equals(relation.getSourcePageId())
                            ? relation.getTargetPageId() : relation.getSourcePageId();
                    PageEntity related = pageRepository.findByIdAndLibraryId(relatedId, page.getLibraryId()).orElse(null);
                    if (related == null) {
                        return null;
                    }
                    PageVersionEntity version = currentVersion(related).orElse(null);
                    return new KnowledgeDtos.RelatedPageResponse(related.getId(), related.getSlug(),
                            version == null ? null : version.getTitle(), relation.getRelationType());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private KnowledgeDtos.ClaimResponse claimView(UUID ownerId, ClaimEntity claim, UUID versionId,
                                                   boolean includeEvidences) {
        ClaimStatusEntity status = versionId == null ? claimStatusRepository.findAllByClaimIdOrderByCreatedAtDesc(claim.getId())
                .stream().findFirst().orElse(null)
                : claimStatusRepository.findByClaimIdAndPageVersionId(claim.getId(), versionId).orElse(null);
        List<EvidenceEntity> evidences = includeEvidences
                ? evidenceRepository.findAllByClaimIdOrderByCreatedAtAsc(claim.getId()) : List.of();
        List<KnowledgeDtos.EvidenceResponse> views = evidences.stream()
                .map(value -> evidenceView(ownerId, value))
                .toList();
        return new KnowledgeDtos.ClaimResponse(claim.getId(), claim.getPageId(), claim.getStatement(),
                status == null ? "HYPOTHESIS" : status.getKnowledgeStatus(),
                status == null ? claim.getAiConfidence() : status.getConfidence(),
                status == null ? null : status.getConditions(), evidences.size(), false, views);
    }

    private KnowledgeDtos.EvidenceResponse evidenceView(UUID ownerId, EvidenceEntity evidence) {
        SourceVersionEntity sourceVersion = sourceVersionRepository.findById(evidence.getSourceVersionId())
                .orElseThrow(() -> notFound("evidence source version not found"));
        SourceEntity source = sourceRepository.findByIdAndOwnerId(sourceVersion.getSourceId(), ownerId)
                .orElseThrow(() -> notFound("evidence source not found"));
        if (evidence.getExcerpt() == null || evidence.getExcerpt().length() > 400
                || evidence.getLocator() == null || evidence.getExcerptLanguage() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_EVIDENCE", "evidence trace is invalid");
        }
        return new KnowledgeDtos.EvidenceResponse(evidence.getId(), evidence.getClaimId(), evidence.getEvidenceType(),
                sourceVersion.getId(), source.getId(), source.getTitle(), evidence.getLocator(), evidence.getExcerpt(),
                evidence.getExcerptLanguage());
    }

    private KnowledgeDtos.RelationResponse relationView(PageRelationEntity relation) {
        return new KnowledgeDtos.RelationResponse(relation.getId(), relation.getLibraryId(), relation.getSourcePageId(),
                relation.getTargetPageId(), relation.getRelationType(), relation.getRationale(), relation.getConfidence(),
                relation.getStatus(), relation.getCreatedByType(), relation.getCreatedAt());
    }

    private KnowledgeDtos.ContradictionResponse contradictionView(UUID libraryId, ContradictionEntity contradiction) {
        String left = claimRepository.findById(contradiction.getLeftClaimId()).map(ClaimEntity::getStatement).orElse(null);
        String right = claimRepository.findById(contradiction.getRightClaimId()).map(ClaimEntity::getStatement).orElse(null);
        return new KnowledgeDtos.ContradictionResponse(contradiction.getId(), libraryId, contradiction.getLeftClaimId(),
                contradiction.getRightClaimId(), left, right, contradiction.getClassification(),
                contradiction.getExplanation(), contradiction.getConfidence(), contradiction.getStatus(),
                contradiction.getResolutionNote(), contradiction.getResolvedAt(), contradiction.getCreatedAt());
    }

    private boolean claimsBelongToLibrary(UUID libraryId, ContradictionEntity contradiction) {
        return claimBelongsToLibrary(libraryId, contradiction.getLeftClaimId())
                && claimBelongsToLibrary(libraryId, contradiction.getRightClaimId());
    }

    private boolean pagesBelongToLibrary(UUID libraryId, PageRelationEntity relation) {
        return pageRepository.findByIdAndLibraryId(relation.getSourcePageId(), libraryId).isPresent()
                && pageRepository.findByIdAndLibraryId(relation.getTargetPageId(), libraryId).isPresent();
    }

    private boolean claimBelongsToLibrary(UUID libraryId, UUID claimId) {
        return claimRepository.findById(claimId)
                .flatMap(claim -> pageRepository.findByIdAndLibraryId(claim.getPageId(), libraryId))
                .isPresent();
    }

    private boolean matchesQuery(PageEntity page, String query) {
        String needle = query.toLowerCase();
        if (page.getSlug().toLowerCase().contains(needle)) {
            return true;
        }
        return currentVersion(page).map(version -> version.getTitle() != null
                && version.getTitle().toLowerCase().contains(needle)).orElse(false);
    }

    private boolean containsTopic(JsonNode topics, String topic) {
        if (topics == null || !topics.isArray()) {
            return false;
        }
        for (JsonNode value : topics) {
            if (topic.equalsIgnoreCase(value.asText())) {
                return true;
            }
        }
        return false;
    }

    private java.util.Optional<PageVersionEntity> currentVersion(PageEntity page) {
        if (page.getCurrentVersionId() != null) {
            return pageVersionRepository.findById(page.getCurrentVersionId())
                    .filter(version -> page.getId().equals(version.getPageId()));
        }
        return pageVersionRepository.findTopByPageIdOrderByVersionNoDesc(page.getId());
    }

    private int latestVersionNo(UUID pageId) {
        return pageVersionRepository.findTopByPageIdOrderByVersionNoDesc(pageId)
                .map(PageVersionEntity::getVersionNo)
                .orElseThrow(() -> notFound("page version not found"));
    }

    private PageEntity requirePage(UUID ownerId, UUID libraryId, UUID pageId) {
        requireLibrary(ownerId, libraryId);
        return pageRepository.findByIdAndLibraryId(pageId, libraryId)
                .orElseThrow(() -> notFound("page not found"));
    }

    private ClaimEntity requireClaim(UUID ownerId, UUID libraryId, UUID claimId) {
        requireLibrary(ownerId, libraryId);
        ClaimEntity claim = claimRepository.findById(claimId).orElseThrow(() -> notFound("claim not found"));
        requirePage(ownerId, libraryId, claim.getPageId());
        return claim;
    }

    private LibraryEntity requireLibrary(UUID ownerId, UUID libraryId) {
        return libraryRepository.findByIdAndOwnerId(libraryId, ownerId)
                .orElseThrow(() -> notFound("library not found"));
    }

    private static KnowledgeDtos.PageVersionResponse versionView(PageVersionEntity version) {
        return new KnowledgeDtos.PageVersionResponse(version.getId(), version.getPageId(), version.getVersionNo(),
                version.getTitle(), version.getMarkdownBody(), version.getSummary(), version.getCreatedAt());
    }

    private static KnowledgeDtos.PageVersionSnapshot snapshot(PageVersionEntity version) {
        return new KnowledgeDtos.PageVersionSnapshot(version.getVersionNo(), version.getTitle(), version.getMarkdownBody());
    }

    private static KnowledgeDtos.ChangeSetResponse changeSetView(ChangeSetEntity changeSet) {
        return new KnowledgeDtos.ChangeSetResponse(changeSet.getId(), changeSet.getStatus(), changeSet.getRiskLevel());
    }

    private static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
