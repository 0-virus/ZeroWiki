package com.zerowiki.knowledge;

import com.zerowiki.common.ApiException;
import com.zerowiki.ingest.IngestJobEntity;
import com.zerowiki.ingest.IngestJobRepository;
import com.zerowiki.library.LibraryEntity;
import com.zerowiki.library.LibraryRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ChangeSetService {
    private static final Set<String> DECISIONS = Set.of("APPROVE", "REJECT", "REQUEST_CHANGES", "DEFER");

    private final LibraryRepository libraryRepository;
    private final ChangeSetRepository changeSetRepository;
    private final ChangeItemRepository changeItemRepository;
    private final ChangeReviewRepository changeReviewRepository;
    private final LibraryVersionRepository libraryVersionRepository;
    private final PageRepository pageRepository;
    private final PageVersionRepository pageVersionRepository;
    private final PageRelationRepository relationRepository;
    private final ContradictionRepository contradictionRepository;
    private final AuditLogRepository auditLogRepository;
    private final IngestJobRepository ingestJobRepository;
    private final ObjectMapper objectMapper;

    public ChangeSetService(LibraryRepository libraryRepository,
                            ChangeSetRepository changeSetRepository,
                            ChangeItemRepository changeItemRepository,
                            ChangeReviewRepository changeReviewRepository,
                            LibraryVersionRepository libraryVersionRepository,
                            PageRepository pageRepository,
                            PageVersionRepository pageVersionRepository,
                            PageRelationRepository relationRepository,
                            ContradictionRepository contradictionRepository,
                            AuditLogRepository auditLogRepository,
                            IngestJobRepository ingestJobRepository,
                            ObjectMapper objectMapper) {
        this.libraryRepository = libraryRepository;
        this.changeSetRepository = changeSetRepository;
        this.changeItemRepository = changeItemRepository;
        this.changeReviewRepository = changeReviewRepository;
        this.libraryVersionRepository = libraryVersionRepository;
        this.pageRepository = pageRepository;
        this.pageVersionRepository = pageVersionRepository;
        this.relationRepository = relationRepository;
        this.contradictionRepository = contradictionRepository;
        this.auditLogRepository = auditLogRepository;
        this.ingestJobRepository = ingestJobRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ChangeSetDtos.ChangeSetResponse> list(UUID ownerId, UUID libraryId,
                                                       String status, String riskLevel) {
        requireLibrary(ownerId, libraryId);
        return changeSetRepository.findAllByLibraryIdOrderByCreatedAtDesc(libraryId).stream()
                .filter(changeSet -> status == null || status.equalsIgnoreCase(changeSet.getStatus()))
                .filter(changeSet -> riskLevel == null || riskLevel.equalsIgnoreCase(changeSet.getRiskLevel()))
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChangeSetDtos.ChangeSetDetailResponse get(UUID ownerId, UUID libraryId, UUID changeSetId) {
        ChangeSetEntity changeSet = requireChangeSet(ownerId, libraryId, changeSetId);
        return detail(changeSet);
    }

    @Transactional(readOnly = true)
    public List<ChangeSetDtos.ChangeItemResponse> items(UUID ownerId, UUID libraryId, UUID changeSetId) {
        ChangeSetEntity changeSet = requireChangeSet(ownerId, libraryId, changeSetId);
        return changeItemRepository.findAllByChangeSetIdOrderByCreatedAtAsc(changeSet.getId()).stream()
                .map(ChangeSetService::itemResponse)
                .toList();
    }

    @Transactional
    public ReviewResult review(UUID ownerId, UUID libraryId, UUID changeSetId,
                               @Valid ChangeSetDtos.ReviewRequest request) {
        ChangeSetEntity changeSet = requireChangeSet(ownerId, libraryId, changeSetId);
        if (request.expectedRevision() != changeSet.getRevision()) {
            throw conflict("change set revision changed");
        }
        Set<UUID> seen = new HashSet<>();
        boolean hasRevisionRequest = false;
        for (ChangeSetDtos.ReviewDecision decision : request.decisions()) {
            if (!seen.add(decision.changeItemId())) {
                throw conflict("change item is reviewed more than once");
            }
            String normalizedDecision = normalizeDecision(decision.decision());
            if (("REJECT".equals(normalizedDecision) || "REQUEST_CHANGES".equals(normalizedDecision))
                    && (decision.comment() == null || decision.comment().isBlank())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "COMMENT_REQUIRED",
                        "comment is required for this review decision");
            }
            ChangeItemEntity item = changeItemRepository.findByIdAndChangeSetId(decision.changeItemId(), changeSetId)
                    .orElseThrow(() -> notFound("change item not found"));
            item.review(normalizedDecision);
            changeItemRepository.save(item);
            changeReviewRepository.save(ChangeReviewEntity.create(changeSetId, item.getId(), ownerId,
                    normalizedDecision, decision.comment()));
            hasRevisionRequest |= "REQUEST_CHANGES".equals(normalizedDecision);
        }
        List<ChangeItemEntity> items = changeItemRepository.findAllByChangeSetIdOrderByCreatedAtAsc(changeSetId);
        changeSet.refreshReviewStatus(items);
        changeSetRepository.save(changeSet);
        return new ReviewResult(detail(changeSet), hasRevisionRequest);
    }

    @Transactional
    public ChangeSetDtos.ApplyResponse apply(UUID ownerId, UUID libraryId, UUID changeSetId,
                                             ChangeSetDtos.ApplyRequest request) {
        LibraryEntity library = requireLibrary(ownerId, libraryId);
        ChangeSetEntity changeSet = requireChangeSet(ownerId, libraryId, changeSetId);
        if (request.expectedChangeSetRevision() != changeSet.getRevision()) {
            throw conflict("change set revision changed");
        }
        LibraryVersionEntity current = libraryVersionRepository.findTopByLibraryIdOrderByVersionNoDesc(libraryId)
                .orElse(null);
        long currentVersionNo = current == null ? 0 : current.getVersionNo();
        if (request.expectedLibraryVersion() != currentVersionNo) {
            throw conflict("library version changed");
        }
        List<ChangeItemEntity> items = changeItemRepository.findAllByChangeSetIdOrderByCreatedAtAsc(changeSetId);
        boolean hasBlockingItem = items.stream().anyMatch(item -> !"APPROVED".equals(item.getReviewStatus())
                && !"REJECTED".equals(item.getReviewStatus()));
        boolean hasApprovedItem = items.stream().anyMatch(item -> "APPROVED".equals(item.getReviewStatus()));
        if (hasBlockingItem) {
            throw new ApiException(HttpStatus.CONFLICT, "CHANGE_SET_NOT_APPROVED",
                    "deferred or revision-requested change items must be resolved before apply");
        }
        if (!hasApprovedItem) {
            throw new ApiException(HttpStatus.CONFLICT, "NO_APPROVED_ITEMS", "no approved change items to apply");
        }
        LibraryVersionEntity version = libraryVersionRepository.save(LibraryVersionEntity.create(
                libraryId, currentVersionNo + 1, changeSetId,
                request.summary() == null || request.summary().isBlank() ? changeSet.getSummary() : request.summary().trim(),
                ownerId));
        for (ChangeItemEntity item : items) {
            if ("APPROVED".equals(item.getReviewStatus())) {
                applyItem(ownerId, libraryId, changeSet, item);
                auditLogRepository.save(auditEvent(ownerId, libraryId, version, changeSet, item));
            }
        }
        library.setCurrentLibraryVersionId(version.getId());
        libraryRepository.save(library);
        changeSet.markApplied();
        changeSetRepository.save(changeSet);
        completeIngestJob(ownerId, libraryId, changeSet);
        return new ChangeSetDtos.ApplyResponse(changeSetId, changeSet.getStatus(), versionResponse(version), null);
    }

    private void completeIngestJob(UUID ownerId, UUID libraryId, ChangeSetEntity changeSet) {
        if (!"INGEST".equals(changeSet.getOriginType()) || changeSet.getOriginId() == null) {
            return;
        }
        IngestJobEntity job = ingestJobRepository
                .findByIdAndLibraryIdAndRequestedBy(changeSet.getOriginId(), libraryId, ownerId)
                .orElseThrow(() -> notFound("ingest job not found"));
        job.complete();
        ingestJobRepository.save(job);
    }

    private void applyItem(UUID ownerId, UUID libraryId, ChangeSetEntity changeSet, ChangeItemEntity item) {
        switch (item.getEntityType()) {
            case "PAGE" -> applyPage(ownerId, libraryId, changeSet, item);
            case "RELATION" -> applyRelation(libraryId, item);
            case "CONTRADICTION" -> applyContradiction(ownerId, libraryId, item);
            default -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_CHANGE_TARGET",
                    "change item target cannot be applied in this slice");
        }
    }

    private void applyPage(UUID ownerId, UUID libraryId, ChangeSetEntity changeSet, ChangeItemEntity item) {
        if ("CREATE".equals(item.getOperation())) {
            applyNewPage(ownerId, libraryId, changeSet, item);
            return;
        }
        PageEntity page = pageRepository.findByIdAndLibraryId(item.getEntityId(), libraryId)
                .orElseThrow(() -> notFound("page not found"));
        JsonNode after = requiredSnapshot(item);
        String status = after.path("status").asText(null);
        if (status != null) {
            if (!"ARCHIVED".equals(status) && !"PUBLISHED".equals(status)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PAGE_CHANGE",
                        "unsupported page status change");
            }
            page.applyStatus(status);
            pageRepository.save(page);
            return;
        }
        String markdown = after.path("proposedMarkdown").asText(null);
        if (markdown == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PAGE_CHANGE",
                    "proposed markdown is required");
        }
        PageVersionEntity current = pageVersionRepository.findTopByPageIdOrderByVersionNoDesc(page.getId())
                .orElseThrow(() -> notFound("current page version not found"));
        int baseVersionNo = after.path("baseVersionNo").asInt(current.getVersionNo());
        if (baseVersionNo != current.getVersionNo()) {
            throw conflict("page version changed");
        }
        String title = after.path("title").asText(current.getTitle());
        PageVersionEntity version = pageVersionRepository.save(PageVersionEntity.create(page.getId(),
                current.getVersionNo() + 1, title, markdown, current.getSummary(), changeSet.getId(), ownerId));
        page.applyVersion(version.getId());
        pageRepository.save(page);
    }

    private void applyNewPage(UUID ownerId, UUID libraryId, ChangeSetEntity changeSet, ChangeItemEntity item) {
        // Ingest projections are written before review; applying their CREATE item is idempotent.
        if (pageRepository.findByIdAndLibraryId(item.getEntityId(), libraryId).isPresent()) {
            return;
        }
        JsonNode after = requiredSnapshot(item);
        String title = after.path("title").asText(null);
        String markdown = after.path("proposedMarkdown").asText(null);
        if (title == null || title.isBlank() || markdown == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PAGE_CHANGE",
                    "title and proposed markdown are required");
        }
        String pageType = after.path("pageType").asText("SYNTHESIS");
        String status = after.path("status").asText("PUBLISHED");
        String slug = title.trim().toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("^-|-$", "");
        if (slug.isBlank()) {
            slug = "page";
        }
        slug = (slug + "-" + item.getEntityId().toString().substring(0, 8));
        if (slug.length() > 240) {
            slug = slug.substring(0, 240);
        }
        PageEntity page = pageRepository.save(PageEntity.createWithId(item.getEntityId(), libraryId, slug,
                pageType, status));
        PageVersionEntity version = pageVersionRepository.save(PageVersionEntity.create(page.getId(), 1, title.trim(),
                markdown, null, changeSet.getId(), ownerId));
        page.applyVersion(version.getId());
        pageRepository.save(page);
    }

    private void applyRelation(UUID libraryId, ChangeItemEntity item) {
        PageRelationEntity relation = relationRepository.findById(item.getEntityId())
                .filter(value -> libraryId.equals(value.getLibraryId()))
                .orElseThrow(() -> notFound("relation not found"));
        JsonNode after = requiredSnapshot(item);
        String status = after.path("status").asText(null);
        if ("ACCEPTED".equals(status)) {
            relation.accept();
            relationRepository.save(relation);
            return;
        }
        String feedbackType = after.path("feedbackType").asText(null);
        if (feedbackType == null || feedbackType.isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_RELATION_CHANGE",
                    "feedback type is required");
        }
        if ("DUPLICATE_CONCEPT".equals(feedbackType)) {
            relation.archive();
        } else {
            relation.reject();
        }
        relationRepository.save(relation);
    }

    private void applyContradiction(UUID ownerId, UUID libraryId, ChangeItemEntity item) {
        ContradictionEntity contradiction = contradictionRepository.findByIdAndLibraryId(item.getEntityId(), libraryId)
                .orElseThrow(() -> notFound("contradiction not found"));
        JsonNode after = requiredSnapshot(item);
        String decision = after.path("decision").asText("CLASSIFY");
        String status = switch (decision) {
            case "DISMISS" -> "DISMISSED";
            case "ACCEPT" -> "ACCEPTED";
            case "CLASSIFY", "RESOLVE" -> "RESOLVED";
            default -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_CONTRADICTION_CHANGE",
                    "unsupported contradiction decision");
        };
        String classification = after.path("classification").asText(null);
        if (classification != null && !Set.of("TRUE_CONFLICT", "TIME_CHANGE", "CONDITION_DIFF",
                "VIEWPOINT_DIFF", "POSSIBLE_CONFLICT").contains(classification)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_CONTRADICTION_CHANGE",
                    "unsupported contradiction classification");
        }
        contradiction.resolve(status, classification, after.path("resolutionNote").asText(null), ownerId);
        contradictionRepository.save(contradiction);
    }

    private JsonNode requiredSnapshot(ChangeItemEntity item) {
        JsonNode snapshot = item.getAfterSnapshot();
        if (snapshot == null || !snapshot.isObject()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_CHANGE_SNAPSHOT",
                    "change item after snapshot is required");
        }
        return snapshot;
    }

    private AuditLogEntity auditEvent(UUID ownerId, UUID libraryId, LibraryVersionEntity version,
                                      ChangeSetEntity changeSet, ChangeItemEntity item) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("changeSetId", changeSet.getId().toString());
        metadata.put("changeItemId", item.getId().toString());
        metadata.put("libraryVersionId", version.getId().toString());
        metadata.put("operation", item.getOperation());
        metadata.put("snapshotSource", "change_items");
        return AuditLogEntity.success(ownerId, libraryId, item.getEntityType(), item.getEntityId(), metadata);
    }

    @Transactional
    public ChangeSetDtos.ChangeSetDetailResponse recalculate(UUID ownerId, UUID libraryId, UUID changeSetId,
                                                               ChangeSetDtos.RecalculateRequest request) {
        LibraryEntity library = requireLibrary(ownerId, libraryId);
        ChangeSetEntity changeSet = requireChangeSet(ownerId, libraryId, changeSetId);
        if (request != null && request.expectedRevision() != null
                && request.expectedRevision() != changeSet.getRevision()) {
            throw conflict("change set revision changed");
        }
        changeSet.recalculate(library.getCurrentLibraryVersionId());
        changeSetRepository.save(changeSet);
        return detail(changeSet);
    }

    @Transactional(readOnly = true)
    public List<ChangeSetDtos.LibraryVersionResponse> versions(UUID ownerId, UUID libraryId) {
        requireLibrary(ownerId, libraryId);
        return libraryVersionRepository.findAllByLibraryIdOrderByVersionNoDesc(libraryId).stream()
                .map(ChangeSetService::versionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChangeSetDtos.LibraryVersionResponse version(UUID ownerId, UUID libraryId, long versionNo) {
        requireLibrary(ownerId, libraryId);
        return versionResponse(libraryVersionRepository.findByLibraryIdAndVersionNo(libraryId, versionNo)
                .orElseThrow(() -> notFound("library version not found")));
    }

    @Transactional(readOnly = true)
    public ChangeSetDtos.VersionDiffResponse diff(UUID ownerId, UUID libraryId, long from, long to) {
        requireLibrary(ownerId, libraryId);
        LibraryVersionEntity fromVersion = libraryVersionRepository.findByLibraryIdAndVersionNo(libraryId, from)
                .orElseThrow(() -> notFound("source library version not found"));
        LibraryVersionEntity toVersion = libraryVersionRepository.findByLibraryIdAndVersionNo(libraryId, to)
                .orElseThrow(() -> notFound("target library version not found"));
        List<ChangeSetDtos.ChangeItemResponse> changes = changeItemRepository
                .findAllByChangeSetIdOrderByCreatedAtAsc(toVersion.getChangeSetId()).stream()
                .map(ChangeSetService::itemResponse)
                .toList();
        return new ChangeSetDtos.VersionDiffResponse(versionResponse(fromVersion), versionResponse(toVersion), changes);
    }

    private ChangeSetEntity requireChangeSet(UUID ownerId, UUID libraryId, UUID changeSetId) {
        requireLibrary(ownerId, libraryId);
        return changeSetRepository.findByIdAndLibraryId(changeSetId, libraryId)
                .orElseThrow(() -> notFound("change set not found"));
    }

    private LibraryEntity requireLibrary(UUID ownerId, UUID libraryId) {
        return libraryRepository.findByIdAndOwnerId(libraryId, ownerId)
                .orElseThrow(() -> notFound("library not found"));
    }

    private ChangeSetDtos.ChangeSetDetailResponse detail(ChangeSetEntity changeSet) {
        List<ChangeItemEntity> items = changeItemRepository.findAllByChangeSetIdOrderByCreatedAtAsc(changeSet.getId());
        ChangeSetDtos.ChangeSetResponse response = response(changeSet, items);
        return new ChangeSetDtos.ChangeSetDetailResponse(response.id(), response.libraryId(), response.title(),
                response.status(), response.riskLevel(), response.summary(), response.baseLibraryVersionNo(),
                response.counts(), response.revision(), response.createdAt(), response.reviewedAt(), response.appliedAt(),
                items.stream().map(ChangeSetService::itemResponse).toList());
    }

    private ChangeSetDtos.ChangeSetResponse response(ChangeSetEntity changeSet) {
        return response(changeSet, changeItemRepository.findAllByChangeSetIdOrderByCreatedAtAsc(changeSet.getId()));
    }

    private ChangeSetDtos.ChangeSetResponse response(ChangeSetEntity changeSet, List<ChangeItemEntity> items) {
        long minor = items.stream().filter(item -> "MINOR".equals(item.getRiskLevel())).count();
        long major = items.stream().filter(item -> "MAJOR".equals(item.getRiskLevel())).count();
        Long baseVersionNo = changeSet.getBaseLibraryVersionId() == null ? null
                : libraryVersionRepository.findByIdAndLibraryId(changeSet.getBaseLibraryVersionId(), changeSet.getLibraryId())
                .map(LibraryVersionEntity::getVersionNo).orElse(null);
        return new ChangeSetDtos.ChangeSetResponse(changeSet.getId(), changeSet.getLibraryId(), changeSet.getTitle(),
                changeSet.getStatus(), changeSet.getRiskLevel(), changeSet.getSummary(), baseVersionNo,
                new ChangeSetDtos.Counts(items.size(), minor, major), changeSet.getRevision(), changeSet.getCreatedAt(),
                changeSet.getReviewedAt(), changeSet.getAppliedAt());
    }

    private static ChangeSetDtos.ChangeItemResponse itemResponse(ChangeItemEntity item) {
        return new ChangeSetDtos.ChangeItemResponse(item.getId(), item.getChangeSetId(), item.getEntityType(),
                item.getEntityId(), item.getOperation(), item.getRiskLevel(), item.getReason(), item.getBeforeSnapshot(),
                item.getAfterSnapshot(), item.getEvidenceSummary(), item.getAiConfidence(), item.getReviewStatus(),
                item.getCreatedAt());
    }

    private static ChangeSetDtos.LibraryVersionResponse versionResponse(LibraryVersionEntity version) {
        return new ChangeSetDtos.LibraryVersionResponse(version.getId(), version.getLibraryId(), version.getVersionNo(),
                version.getChangeSetId(), version.getSummary(), version.getCreatedBy(), version.getCreatedAt());
    }

    private String normalizeDecision(String decision) {
        String normalized = decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
        if (!DECISIONS.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REVIEW_DECISION", "unsupported review decision");
        }
        return normalized;
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", message);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    public record ReviewResult(ChangeSetDtos.ChangeSetDetailResponse response, boolean accepted) {
    }
}
