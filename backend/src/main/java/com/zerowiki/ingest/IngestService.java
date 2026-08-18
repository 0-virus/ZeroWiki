package com.zerowiki.ingest;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import com.zerowiki.common.ApiException;
import com.zerowiki.library.LibraryRepository;
import com.zerowiki.knowledge.ChangeItemEntity;
import com.zerowiki.knowledge.ChangeSetEntity;
import com.zerowiki.knowledge.ChangeItemRepository;
import com.zerowiki.knowledge.ChangeSetRepository;
import com.zerowiki.knowledge.ClaimEntity;
import com.zerowiki.knowledge.ClaimRepository;
import com.zerowiki.knowledge.ClaimStatusEntity;
import com.zerowiki.knowledge.ClaimStatusRepository;
import com.zerowiki.knowledge.EvidenceEntity;
import com.zerowiki.knowledge.EvidenceRepository;
import com.zerowiki.knowledge.PageEntity;
import com.zerowiki.knowledge.PageRelationEntity;
import com.zerowiki.knowledge.PageRelationRepository;
import com.zerowiki.knowledge.PageRepository;
import com.zerowiki.knowledge.PageVersionEntity;
import com.zerowiki.knowledge.PageVersionRepository;
import com.zerowiki.source.SourceEntity;
import com.zerowiki.source.SourceService;
import com.zerowiki.source.SourceVersionEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class IngestService {
    private final IngestJobRepository jobRepository;
    private final IngestItemRepository itemRepository;
    private final JobQuestionRepository questionRepository;
    private final LibraryRepository libraryRepository;
    private final SourceService sourceService;
    private final ObjectMapper objectMapper;
    private final PageRepository pageRepository;
    private final PageVersionRepository pageVersionRepository;
    private final ClaimRepository claimRepository;
    private final ClaimStatusRepository claimStatusRepository;
    private final EvidenceRepository evidenceRepository;
    private final PageRelationRepository relationRepository;
    private final ChangeSetRepository changeSetRepository;
    private final ChangeItemRepository changeItemRepository;

    public IngestService(IngestJobRepository jobRepository,
                         IngestItemRepository itemRepository,
                         JobQuestionRepository questionRepository,
                         LibraryRepository libraryRepository,
                         SourceService sourceService,
                         ObjectMapper objectMapper,
                         PageRepository pageRepository,
                         PageVersionRepository pageVersionRepository,
                         ClaimRepository claimRepository,
                         ClaimStatusRepository claimStatusRepository,
                         EvidenceRepository evidenceRepository,
                         PageRelationRepository relationRepository,
                         ChangeSetRepository changeSetRepository,
                         ChangeItemRepository changeItemRepository) {
        this.jobRepository = jobRepository;
        this.itemRepository = itemRepository;
        this.questionRepository = questionRepository;
        this.libraryRepository = libraryRepository;
        this.sourceService = sourceService;
        this.objectMapper = objectMapper;
        this.pageRepository = pageRepository;
        this.pageVersionRepository = pageVersionRepository;
        this.claimRepository = claimRepository;
        this.claimStatusRepository = claimStatusRepository;
        this.evidenceRepository = evidenceRepository;
        this.relationRepository = relationRepository;
        this.changeSetRepository = changeSetRepository;
        this.changeItemRepository = changeItemRepository;
    }

    @Transactional
    public IngestJobEntity create(UUID ownerId, UUID libraryId, IngestDtos.CreateIngestRequest request) {
        requireLibrary(ownerId, libraryId);
        if (request.importType() == ImportType.NOTION || request.importType() == ImportType.URL) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DEPENDENCY_UNAVAILABLE",
                    "this import type is not available in the M2 slice");
        }
        Set<UUID> uniqueIds = new HashSet<>(request.sourceVersionIds());
        if (uniqueIds.size() != request.sourceVersionIds().size()) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_SOURCE", "source version is duplicated");
        }
        List<SourceVersionEntity> versions = sourceService.ownedVersions(ownerId, request.sourceVersionIds());
        long estimatedTokens = versions.stream().mapToLong(version -> Math.max(1, (version.getByteSize() + 3) / 4)).sum();
        ObjectNode plan = objectMapper.createObjectNode();
        plan.put("documents", versions.size());
        plan.put("processable", versions.size());
        plan.put("duplicates", 0);
        plan.put("heldForReview", 0);
        plan.put("topicGroups", 0);
        plan.put("estimatedTokens", estimatedTokens);
        plan.put("estimatedBillableUnits", estimatedTokens);
        IngestJobEntity job = jobRepository.save(IngestJobEntity.create(libraryId, ownerId, request.importType(),
                request.processingMode(), plan, estimatedTokens, versions.size()));
        for (SourceVersionEntity version : versions) {
            itemRepository.save(IngestItemEntity.create(job.getId(), version.getId(), objectMapper.createArrayNode()));
        }
        return job;
    }

    @Transactional(readOnly = true)
    public List<IngestJobEntity> list(UUID ownerId, UUID libraryId) {
        requireLibrary(ownerId, libraryId);
        return jobRepository.findAllByLibraryIdAndRequestedByOrderByCreatedAtDesc(libraryId, ownerId);
    }

    @Transactional(readOnly = true)
    public IngestJobEntity get(UUID ownerId, UUID libraryId, UUID jobId) {
        requireLibrary(ownerId, libraryId);
        return jobRepository.findByIdAndLibraryIdAndRequestedBy(jobId, libraryId, ownerId)
                .orElseThrow(() -> notFound("ingest job not found"));
    }

    @Transactional(readOnly = true)
    public List<IngestItemEntity> items(UUID ownerId, UUID libraryId, UUID jobId) {
        get(ownerId, libraryId, jobId);
        return itemRepository.findAllByIngestJobIdOrderByCreatedAtAsc(jobId);
    }

    @Transactional
    public IngestJobEntity approvePlan(UUID ownerId, UUID libraryId, UUID jobId,
                                       IngestDtos.PlanApprovalRequest request) {
        IngestJobEntity job = get(ownerId, libraryId, jobId);
        if (request.expectedPlanRevision() != null && request.expectedPlanRevision() != job.getPlanRevision()) {
            throw new ApiException(HttpStatus.CONFLICT, "PLAN_CHANGED", "ingest plan revision changed");
        }
        if (request.expectedEstimatedTokens() != null
                && request.expectedEstimatedTokens() != job.getEstimatedTokens()) {
            throw new ApiException(HttpStatus.CONFLICT, "PLAN_CHANGED", "estimated tokens changed");
        }
        if (request.processingMode() != null && request.processingMode() != job.getProcessingMode()) {
            throw new ApiException(HttpStatus.CONFLICT, "PLAN_CHANGED", "processing mode changed");
        }
        validateIncludedItems(job, request.includedItemIds());
        if (request.decision() == IngestDtos.PlanDecision.APPROVE) {
            transition(job, job::approvePlan);
        } else {
            transition(job, job::rejectPlan);
        }
        return jobRepository.save(job);
    }

    @Transactional
    public IngestJobEntity cancel(UUID ownerId, UUID libraryId, UUID jobId) {
        IngestJobEntity job = get(ownerId, libraryId, jobId);
        transition(job, job::cancel);
        return jobRepository.save(job);
    }

    @Transactional
    public IngestJobEntity retry(UUID ownerId, UUID libraryId, UUID jobId) {
        IngestJobEntity job = get(ownerId, libraryId, jobId);
        List<IngestItemEntity> failed = itemRepository.findAllByIngestJobIdAndStatus(jobId, IngestItemStatus.FAILED);
        if (failed.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "NO_FAILED_ITEMS", "no failed ingest items to retry");
        }
        failed.forEach(item -> {
            item.resetForRetry();
            itemRepository.save(item);
        });
        transition(job, job::retry);
        job.markProgress(Math.min(job.getProgressCompleted(), job.getProgressTotal()));
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public List<JobQuestionEntity> questions(UUID ownerId, UUID libraryId, UUID jobId) {
        get(ownerId, libraryId, jobId);
        return questionRepository.findAllByIngestJobIdOrderByCreatedAtAsc(jobId);
    }

    @Transactional
    public IngestJobEntity answer(UUID ownerId, UUID libraryId, UUID jobId, UUID questionId,
                                 IngestDtos.AnswerRequest request) {
        IngestJobEntity job = get(ownerId, libraryId, jobId);
        JobQuestionEntity question = questionRepository.findByIdAndIngestJobId(questionId, jobId)
                .orElseThrow(() -> notFound("job question not found"));
        if (request.status() == QuestionStatus.ANSWERED) {
            if (request.answer() == null || request.answer().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "answer is required");
            }
            question.answer(request.answer().trim());
        } else if (request.status() == QuestionStatus.SKIPPED) {
            question.skip();
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATE_TRANSITION", "question must be resolved");
        }
        questionRepository.save(question);
        if (job.getStatus() == IngestStatus.QUESTION_REVIEW
                && questionRepository.findAllByIngestJobIdOrderByCreatedAtAsc(jobId).stream()
                .noneMatch(item -> item.getStatus() == QuestionStatus.OPEN)) {
            transition(job, job::resumeAfterQuestions);
            jobRepository.save(job);
        }
        return job;
    }

    @Transactional
    public IngestJobEntity answerBulk(UUID ownerId, UUID libraryId, UUID jobId,
                                      IngestDtos.BulkAnswerRequest request) {
        IngestJobEntity job = get(ownerId, libraryId, jobId);
        if (job.getStatus() != IngestStatus.QUESTION_REVIEW) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATE_TRANSITION",
                    "ingest job is not waiting for answers");
        }
        Set<UUID> questionIds = new HashSet<>();
        for (IngestDtos.AnswerEntry answer : request.answers()) {
            if (!questionIds.add(answer.questionId())) {
                throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_QUESTION", "question is answered more than once");
            }
            JobQuestionEntity question = questionRepository.findByIdAndIngestJobId(answer.questionId(), jobId)
                    .orElseThrow(() -> notFound("job question not found"));
            question.answer(answer.answer().trim());
            questionRepository.save(question);
        }
        if (questionRepository.findAllByIngestJobIdOrderByCreatedAtAsc(jobId).stream()
                .noneMatch(question -> question.getStatus() == QuestionStatus.OPEN)) {
            transition(job, job::resumeAfterQuestions);
        }
        return jobRepository.save(job);
    }

    @Transactional
    public IngestJobEntity scan(UUID jobId) {
        IngestJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> notFound("ingest job not found"));
        transition(job, () -> job.moveToPlanReview(job.getPlan(), job.getEstimatedTokens()));
        return jobRepository.save(job);
    }

    @Transactional
    public boolean processOne(UUID jobId) {
        IngestJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> notFound("ingest job not found"));
        if (job.getStatus() != IngestStatus.PROCESSING) {
            return false;
        }
        List<IngestItemEntity> pending = itemRepository.findAllByIngestJobIdAndStatus(jobId, IngestItemStatus.PENDING);
        if (pending.isEmpty()) {
            if (job.getProgressCompleted() == job.getProgressTotal()) {
                job.moveToChangeReview();
                jobRepository.save(job);
                return true;
            }
            return false;
        }
        IngestItemEntity item = pending.get(0);
        try {
            project(job, item);
            item.markDone();
        } catch (RuntimeException exception) {
            item.markFailed("PROJECTION_FAILED", safeFailureMessage(exception));
        }
        itemRepository.save(item);
        job.markProgress(job.getProgressCompleted() + 1);
        if (job.getProgressCompleted() == job.getProgressTotal()) {
            ensureChangeSet(job);
            job.moveToChangeReview();
        }
        jobRepository.save(job);
        return true;
    }

    private void project(IngestJobEntity job, IngestItemEntity item) {
        SourceVersionEntity sourceVersion = sourceService.versionById(job.getRequestedBy(), item.getSourceVersionId());
        SourceEntity source = sourceService.get(job.getRequestedBy(), sourceVersion.getSourceId());
        String sourceText = readText(sourceVersion);
        String summary = firstLine(sourceText);
        String sourceTitle = source.getTitle() == null || source.getTitle().isBlank()
                ? "원본 " + sourceVersion.getVersionNo() : source.getTitle().trim();
        String conceptTitle = sourceTitle + " 핵심 주제";
        String sourceSlug = "source-" + source.getId() + "-v" + sourceVersion.getVersionNo();
        String conceptSlug = "concept-" + source.getId() + "-v" + sourceVersion.getVersionNo();
        PageEntity sourcePage = ensurePage(job, sourceSlug, "SOURCE", sourceTitle, sourceText, summary);
        PageEntity conceptPage = ensurePage(job, conceptSlug, "CONCEPT", conceptTitle,
                "# " + conceptTitle + "\n\n" + sourceText, summary);
        PageVersionEntity conceptVersion = pageVersionRepository.findTopByPageIdOrderByVersionNoDesc(conceptPage.getId())
                .orElseThrow(() -> new IllegalStateException("concept page version was not created"));
        String claimKey = "important-" + sourceVersion.getContentHash().substring(0, 32);
        ClaimEntity claim = claimRepository.findByPageIdAndClaimKey(conceptPage.getId(), claimKey)
                .orElseGet(() -> claimRepository.save(ClaimEntity.create(conceptPage.getId(), claimKey,
                        "이 문서는 " + sourceTitle + "의 핵심 내용을 담고 있다.", BigDecimal.valueOf(0.8))));
        if (claimStatusRepository.findByClaimIdAndPageVersionId(claim.getId(), conceptVersion.getId()).isEmpty()) {
            claimStatusRepository.save(ClaimStatusEntity.create(claim.getId(), conceptVersion.getId(),
                    "SOURCE_CLAIM", BigDecimal.valueOf(0.8), objectMapper.createObjectNode()));
        }
        if (evidenceRepository.findByClaimIdAndSourceVersionId(claim.getId(), sourceVersion.getId()).isEmpty()) {
            ObjectNode locator = objectMapper.createObjectNode();
            locator.put("characterStart", 0);
            locator.put("characterEnd", Math.min(sourceText.length(), 400));
            String language = sourceVersion.getDetectedLanguage() == null
                    || sourceVersion.getDetectedLanguage().isBlank() ? "und" : sourceVersion.getDetectedLanguage();
            evidenceRepository.save(EvidenceEntity.create(claim.getId(), sourceVersion.getId(), "SUPPORTS", locator,
                    excerpt(sourceText), language, BigDecimal.valueOf(0.8)));
        }
        if (relationRepository.findByLibraryIdAndSourcePageIdAndTargetPageIdAndRelationType(
                job.getLibraryId(), sourcePage.getId(), conceptPage.getId(), "EXPLAINS").isEmpty()) {
            relationRepository.save(PageRelationEntity.proposed(job.getLibraryId(), sourcePage.getId(),
                    conceptPage.getId(), "EXPLAINS", "원본에서 결정적으로 생성한 주제 연결", BigDecimal.valueOf(0.8)));
        }
    }

    private PageEntity ensurePage(IngestJobEntity job, String slug, String pageType, String title,
                                  String body, String summary) {
        PageEntity page = pageRepository.findByLibraryIdAndSlug(job.getLibraryId(), slug).orElse(null);
        if (page == null) {
            page = pageRepository.save(PageEntity.create(job.getLibraryId(), slug, pageType, "DRAFT"));
        }
        if (pageVersionRepository.findTopByPageIdOrderByVersionNoDesc(page.getId()).isEmpty()) {
            PageVersionEntity version = pageVersionRepository.save(PageVersionEntity.imported(page.getId(), 1,
                    title, body, summary, null));
            page.applyVersion(version.getId());
            pageRepository.save(page);
        }
        return page;
    }

    private void ensureChangeSet(IngestJobEntity job) {
        if (changeSetRepository.findByLibraryIdAndOriginTypeAndOriginId(job.getLibraryId(), "INGEST", job.getId())
                .isPresent()) {
            return;
        }
        ChangeSetEntity changeSet = changeSetRepository.save(ChangeSetEntity.ready(job.getLibraryId(), "INGEST",
                job.getId(), "Import knowledge projection", "Deterministic source and concept projection", "MINOR"));
        for (IngestItemEntity item : itemRepository.findAllByIngestJobIdOrderByCreatedAtAsc(job.getId())) {
            SourceVersionEntity sourceVersion = sourceService.versionById(job.getRequestedBy(), item.getSourceVersionId());
            String sourceSlug = "source-" + sourceVersion.getSourceId() + "-v" + sourceVersion.getVersionNo();
            String conceptSlug = "concept-" + sourceVersion.getSourceId() + "-v" + sourceVersion.getVersionNo();
            addProjectionItem(changeSet, job.getLibraryId(), sourceSlug);
            addProjectionItem(changeSet, job.getLibraryId(), conceptSlug);
            addProjectionRelationItem(changeSet, job.getLibraryId(), sourceSlug, conceptSlug);
        }
    }

    private void addProjectionItem(ChangeSetEntity changeSet, UUID libraryId, String slug) {
        PageEntity page = pageRepository.findByLibraryIdAndSlug(libraryId, slug).orElse(null);
        if (page == null) {
            return;
        }
        PageVersionEntity version = pageVersionRepository.findTopByPageIdOrderByVersionNoDesc(page.getId()).orElse(null);
        if (version == null) {
            return;
        }
        ObjectNode before = objectMapper.createObjectNode();
        before.put("pageType", page.getPageType());
        before.put("status", page.getStatus());
        before.put("title", version.getTitle());
        before.put("baseVersionNo", version.getVersionNo());
        before.put("proposedMarkdown", version.getMarkdownBody());
        ObjectNode after = objectMapper.createObjectNode();
        after.put("pageType", page.getPageType());
        after.put("status", "PUBLISHED");
        after.put("title", version.getTitle());
        after.put("baseVersionNo", version.getVersionNo());
        after.put("proposedMarkdown", version.getMarkdownBody());
        changeItemRepository.save(ChangeItemEntity.update(changeSet.getId(), "PAGE", page.getId(), before, after));
    }

    private void addProjectionRelationItem(ChangeSetEntity changeSet, UUID libraryId, String sourceSlug,
                                           String conceptSlug) {
        PageEntity sourcePage = pageRepository.findByLibraryIdAndSlug(libraryId, sourceSlug).orElse(null);
        PageEntity conceptPage = pageRepository.findByLibraryIdAndSlug(libraryId, conceptSlug).orElse(null);
        if (sourcePage == null || conceptPage == null) {
            return;
        }
        PageRelationEntity relation = relationRepository
                .findByLibraryIdAndSourcePageIdAndTargetPageIdAndRelationType(
                        libraryId, sourcePage.getId(), conceptPage.getId(), "EXPLAINS")
                .orElse(null);
        if (relation == null) {
            return;
        }
        ObjectNode before = objectMapper.createObjectNode();
        before.put("status", relation.getStatus());
        ObjectNode after = objectMapper.createObjectNode();
        after.put("status", "ACCEPTED");
        after.put("relationType", relation.getRelationType());
        changeItemRepository.save(ChangeItemEntity.update(changeSet.getId(), "RELATION", relation.getId(), before, after));
    }

    private String readText(SourceVersionEntity sourceVersion) {
        try (InputStream input = sourceService.open(sourceVersion)) {
            byte[] bytes = input.readNBytes(200_000);
            return new String(bytes, StandardCharsets.UTF_8).replace("\u0000", "").trim();
        } catch (IOException exception) {
            throw new IllegalStateException("source content could not be read", exception);
        }
    }

    private String firstLine(String text) {
        String line = text.lines().map(String::trim).filter(value -> !value.isBlank()).findFirst()
                .orElse("원본에서 추출한 내용");
        return line.replaceFirst("^#+\\s*", "").substring(0, Math.min(line.replaceFirst("^#+\\s*", "").length(), 400));
    }

    private String excerpt(String text) {
        return text.substring(0, Math.min(text.length(), 400));
    }

    private String safeFailureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "문서 처리에 실패했습니다.";
        }
        return message.substring(0, Math.min(message.length(), 500));
    }

    private void validateIncludedItems(IngestJobEntity job, List<UUID> includedItemIds) {
        if (includedItemIds == null || includedItemIds.isEmpty()) {
            return;
        }
        Set<UUID> all = itemRepository.findAllByIngestJobIdOrderByCreatedAtAsc(job.getId()).stream()
                .map(IngestItemEntity::getId).collect(java.util.stream.Collectors.toSet());
        if (!all.containsAll(includedItemIds)) {
            throw new ApiException(HttpStatus.CONFLICT, "PLAN_CHANGED", "included item is not in the plan");
        }
    }

    private void requireLibrary(UUID ownerId, UUID libraryId) {
        libraryRepository.findByIdAndOwnerId(libraryId, ownerId)
                .orElseThrow(() -> notFound("library not found"));
    }

    private void transition(IngestJobEntity job, Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATE_TRANSITION", exception.getMessage());
        }
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
