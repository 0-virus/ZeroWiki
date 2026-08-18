package com.zerowiki.assistant;

import com.zerowiki.common.ApiException;
import com.zerowiki.knowledge.ChangeItemEntity;
import com.zerowiki.knowledge.ChangeItemRepository;
import com.zerowiki.knowledge.ChangeSetEntity;
import com.zerowiki.knowledge.ChangeSetRepository;
import com.zerowiki.knowledge.EvidenceEntity;
import com.zerowiki.knowledge.EvidenceRepository;
import com.zerowiki.knowledge.PageVersionEntity;
import com.zerowiki.knowledge.PageVersionRepository;
import com.zerowiki.library.LibraryEntity;
import com.zerowiki.library.LibraryRepository;
import com.zerowiki.search.SearchDtos;
import com.zerowiki.search.SearchService;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;

@Service
public class AssistantService {
    private static final Set<String> ANSWER_MODES = Set.of("QUICK", "COMPARE", "LEARN", "DECIDE", "REPORT");
    private static final Set<String> RATINGS = Set.of("HELPFUL", "NOT_HELPFUL");
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;

    private final LibraryRepository libraryRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageCitationRepository citationRepository;
    private final FeedbackEventRepository feedbackRepository;
    private final SearchService searchService;
    private final ChangeSetRepository changeSetRepository;
    private final ChangeItemRepository changeItemRepository;
    private final PageVersionRepository pageVersionRepository;
    private final EvidenceRepository evidenceRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public AssistantService(LibraryRepository libraryRepository,
                            ConversationRepository conversationRepository,
                            MessageRepository messageRepository,
                            MessageCitationRepository citationRepository,
                            FeedbackEventRepository feedbackRepository,
                            SearchService searchService,
                            ChangeSetRepository changeSetRepository,
                            ChangeItemRepository changeItemRepository,
                            PageVersionRepository pageVersionRepository,
                            EvidenceRepository evidenceRepository,
                            IdempotencyRecordRepository idempotencyRepository,
                            ObjectMapper objectMapper) {
        this.libraryRepository = libraryRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.citationRepository = citationRepository;
        this.feedbackRepository = feedbackRepository;
        this.searchService = searchService;
        this.changeSetRepository = changeSetRepository;
        this.changeItemRepository = changeItemRepository;
        this.pageVersionRepository = pageVersionRepository;
        this.evidenceRepository = evidenceRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConversationEntity createConversation(UUID ownerId, UUID libraryId, String title) {
        requireLibrary(ownerId, libraryId);
        return conversationRepository.save(ConversationEntity.create(libraryId, ownerId, title));
    }

    @Transactional(readOnly = true)
    public List<AssistantDtos.ConversationResponse> conversations(UUID ownerId, UUID libraryId) {
        requireLibrary(ownerId, libraryId);
        return conversationRepository.findAllByLibraryIdAndUserIdOrderByUpdatedAtDesc(libraryId, ownerId).stream()
                .map(AssistantDtos.ConversationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AssistantDtos.ConversationDetailResponse conversation(UUID ownerId, UUID libraryId,
                                                                  UUID conversationId) {
        ConversationEntity conversation = requireConversation(ownerId, libraryId, conversationId);
        List<AssistantDtos.MessageResponse> messages = messageRepository
                .findAllByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(this::messageResponse)
                .toList();
        return new AssistantDtos.ConversationDetailResponse(AssistantDtos.ConversationResponse.from(conversation), messages);
    }

    @Transactional
    public AssistantDtos.QueuedResponse ask(UUID ownerId, UUID libraryId, UUID conversationId,
                                             AssistantDtos.AskRequest request) {
        return ask(ownerId, libraryId, conversationId, request, null);
    }

    @Transactional
    public AssistantDtos.QueuedResponse ask(UUID ownerId, UUID libraryId, UUID conversationId,
                                             AssistantDtos.AskRequest request, String rawIdempotencyKey) {
        ConversationEntity conversation = requireConversation(ownerId, libraryId, conversationId);
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw badRequest("content is required");
        }
        String answerMode = normalizeAnswerMode(request.answerMode());
        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);
        IdempotencyRecordEntity record = null;
        if (idempotencyKey != null) {
            String requestPath = askPath(libraryId, conversationId);
            String requestHash = requestHash(request);
            java.util.Optional<IdempotencyRecordEntity> existing = idempotencyRepository
                    .findByUserIdAndRequestPathAndIdempotencyKey(ownerId, requestPath, idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRecordEntity value = existing.get();
                if (value.getExpiresAt() != null && !value.getExpiresAt().isAfter(Instant.now())) {
                    idempotencyRepository.delete(value);
                    idempotencyRepository.flush();
                } else {
                    if (!MessageDigest.isEqual(value.getRequestHash().getBytes(StandardCharsets.UTF_8),
                            requestHash.getBytes(StandardCharsets.UTF_8))) {
                        throw conflict("IDEMPOTENCY_KEY_REUSED", "same key was used with a different request");
                    }
                    if (!"COMPLETED".equals(value.getStatus())) {
                        throw conflict("IDEMPOTENCY_KEY_IN_PROGRESS", "the original request is still in progress");
                    }
                    return new AssistantDtos.QueuedResponse(value.getResponseUserMessageId(),
                            value.getResponseAssistantMessageId(), value.getResponseStatus());
                }
            }
            record = IdempotencyRecordEntity.inProgress(ownerId, requestPath, idempotencyKey, requestHash,
                    Instant.now().plus(IDEMPOTENCY_TTL));
            idempotencyRepository.save(record);
        }
        MessageEntity userMessage = messageRepository.save(MessageEntity.user(conversation.getId(),
                request.content(), answerMode));
        MessageEntity assistantMessage = MessageEntity.assistant(conversation.getId(), answerMode);
        List<SearchDtos.SearchResult> results = searchResults(ownerId, libraryId, request);
        List<CitationDraft> drafts = citationDrafts(results);
        boolean insufficient = drafts.isEmpty();
        String answer = insufficient ? insufficientAnswer() : answer(request.content(), drafts);
        assistantMessage.complete(answer, insufficient, Math.max(1, answer.length() / 4));
        messageRepository.save(assistantMessage);
        for (CitationDraft draft : drafts) {
            citationRepository.save(MessageCitationEntity.create(assistantMessage.getId(), draft.result().pageVersionId(),
                    draft.result().claimId(), draft.evidence().getId(), draft.result().sourceVersionId(),
                    drafts.indexOf(draft) + 1, draft.evidence().getExcerpt()));
        }
        if (record != null) {
            record.complete(userMessage.getId(), assistantMessage.getId(), 202, "QUEUED");
            idempotencyRepository.save(record);
        }
        return new AssistantDtos.QueuedResponse(userMessage.getId(), assistantMessage.getId(), "QUEUED");
    }

    @Transactional(readOnly = true)
    public AssistantDtos.MessageResponse message(UUID ownerId, UUID libraryId, UUID conversationId, UUID messageId) {
        requireConversation(ownerId, libraryId, conversationId);
        return messageResponse(messageRepository.findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(() -> notFound("message not found")));
    }

    @Transactional
    public AssistantDtos.SaveProposalResponse saveProposal(UUID ownerId, UUID libraryId, UUID messageId,
                                                           AssistantDtos.SaveProposalRequest request) {
        requireLibrary(ownerId, libraryId);
        MessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> notFound("message not found"));
        ConversationEntity conversation = requireConversation(ownerId, libraryId, message.getConversationId());
        if (!conversation.getId().equals(message.getConversationId()) || !"ASSISTANT".equals(message.getRole())) {
            throw notFound("assistant message not found");
        }
        if (request == null || request.target() == null || request.target().title() == null
                || request.target().title().isBlank()) {
            throw badRequest("target title is required");
        }
        if (!"NEW_PAGE".equalsIgnoreCase(value(request.target().type(), ""))) {
            throw badRequest("only NEW_PAGE proposals are supported");
        }
        String pageType = value(request.target().pageType(), "SYNTHESIS").toUpperCase(Locale.ROOT);
        if (!Set.of("SYNTHESIS", "CONCEPT", "SOURCE").contains(pageType)) {
            throw badRequest("unsupported proposal page type");
        }
        List<UUID> requestedCitationIds = request.includeCitationIds() == null ? List.of() : request.includeCitationIds();
        Set<UUID> available = citationRepository.findAllByMessageIdOrderByCitationOrderAsc(messageId).stream()
                .map(MessageCitationEntity::getId).collect(java.util.stream.Collectors.toSet());
        if (!available.containsAll(requestedCitationIds)) {
            throw badRequest("proposal citation is not attached to this message");
        }
        ChangeSetEntity changeSet = changeSetRepository.save(ChangeSetEntity.ready(libraryId, "ASSISTANT", messageId,
                "Assistant save proposal", "Explicit user proposal from an assistant answer", "MINOR"));
        UUID proposedPageId = UUID.randomUUID();
        ObjectNode after = objectMapper.createObjectNode();
        after.put("pageType", pageType);
        after.put("status", "PUBLISHED");
        after.put("title", request.target().title().trim());
        after.put("baseVersionNo", 0);
        after.put("proposedMarkdown", message.getContent());
        ArrayNode citations = after.putArray("citationIds");
        requestedCitationIds.forEach(id -> citations.add(id.toString()));
        changeItemRepository.save(ChangeItemEntity.create(changeSet.getId(), "PAGE", proposedPageId, after));
        return new AssistantDtos.SaveProposalResponse(changeSet.getId(), List.of(proposedPageId));
    }

    @Transactional
    public boolean feedback(UUID ownerId, UUID libraryId, UUID messageId, AssistantDtos.FeedbackRequest request) {
        if (request == null || (blank(request.rating()) && blank(request.comment()))) {
            return false;
        }
        MessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> notFound("message not found"));
        ConversationEntity conversation = requireConversation(ownerId, libraryId, message.getConversationId());
        if (!conversation.getId().equals(message.getConversationId())) {
            throw notFound("message not found");
        }
        String rating = normalizeRating(request.rating());
        feedbackRepository.save(FeedbackEventEntity.answer(ownerId, libraryId, messageId, rating, request.comment()));
        return true;
    }

    private List<SearchDtos.SearchResult> searchResults(UUID ownerId, UUID libraryId,
                                                        AssistantDtos.AskRequest request) {
        // Answers can cite only traceable claims; PAGE/SOURCE candidates can consume the limit first.
        Set<String> types = Set.of("CLAIM");
        List<SearchDtos.SearchResult> results = new ArrayList<>();
        AssistantDtos.SearchScope scope = request.searchScope();
        boolean current = scope == null || scope.currentLibrary() == null || scope.currentLibrary();
        if (current) {
            results.addAll(searchService.results(ownerId, libraryId, request.content(), types, "KEYWORD", 8, "CURRENT"));
        }
        if (scope != null && scope.referencedLibraryIds() != null) {
            for (UUID referencedId : new LinkedHashSet<>(scope.referencedLibraryIds())) {
                // SearchService verifies owner and the explicit library_references edge.
                results.addAll(searchService.referencedResults(ownerId, libraryId, referencedId, request.content(),
                        types, "KEYWORD", 8));
            }
        }
        return results;
    }

    private List<CitationDraft> citationDrafts(List<SearchDtos.SearchResult> results) {
        Set<UUID> seenClaims = new HashSet<>();
        List<CitationDraft> drafts = new ArrayList<>();
        for (SearchDtos.SearchResult result : results) {
            if (result.claimId() == null || result.pageVersionId() == null || result.sourceVersionId() == null
                    || result.locator() == null || !seenClaims.add(result.claimId())) {
                continue;
            }
            EvidenceEntity evidence = evidenceRepository.findByClaimIdAndSourceVersionId(result.claimId(),
                    result.sourceVersionId()).orElse(null);
            if (evidence != null) {
                drafts.add(new CitationDraft(result, evidence));
            }
            if (drafts.size() == 5) {
                break;
            }
        }
        return drafts;
    }

    private AssistantDtos.MessageResponse messageResponse(MessageEntity message) {
        List<MessageCitationEntity> entities = citationRepository.findAllByMessageIdOrderByCitationOrderAsc(message.getId());
        List<AssistantDtos.Citation> citations = entities.stream().map(citation -> {
            PageVersionEntity pageVersion = citation.getPageVersionId() == null ? null
                    : pageVersionRepository.findById(citation.getPageVersionId()).orElse(null);
            EvidenceEntity evidence = citation.getEvidenceId() == null ? null
                    : evidenceRepository.findById(citation.getEvidenceId()).orElse(null);
            return new AssistantDtos.Citation(citation.getId(), citation.getCitationOrder(),
                    pageVersion == null ? null : pageVersion.getPageId(),
                    pageVersion == null ? null : pageVersion.getTitle(), citation.getClaimId(),
                    citation.getSourceVersionId(), evidence == null ? null : evidence.getLocator());
        }).toList();
        AssistantDtos.Suggestions suggestions = new AssistantDtos.Suggestions(
                message.isInsufficientEvidence() ? List.of("관련 페이지 또는 원본 근거") : List.of(),
                message.isInsufficientEvidence(), false);
        return new AssistantDtos.MessageResponse(message.getId(), message.getConversationId(), message.getRole(),
                message.getStatus(), message.getAnswerMode(), message.getContent(), message.isInsufficientEvidence(),
                citations, suggestions, message.getCompletedAt());
    }

    private String answer(String question, List<CitationDraft> drafts) {
        String titles = drafts.stream().map(draft -> draft.result().title()).distinct().limit(3)
                .reduce((left, right) -> left + ", " + right).orElse("관련 페이지");
        return "질문을 외부 모델에 전송하지 않고 현재 도서관의 근거만으로 답변합니다. "
                + "관련 자료(" + titles + ")를 확인하세요. 질문: " + question.trim();
    }

    private String insufficientAnswer() {
        return "현재 도서관에서 질문을 뒷받침할 근거가 부족해 알 수 없습니다. "
                + "관련 자료를 추가하거나 연결된 도서관 확장 검색을 검토해 주세요.";
    }

    private String normalizeAnswerMode(String value) {
        String normalized = value == null || value.isBlank() ? "QUICK" : value.trim().toUpperCase(Locale.ROOT);
        if (!ANSWER_MODES.contains(normalized)) {
            throw badRequest("unsupported answer mode");
        }
        return normalized;
    }

    private String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw badRequest("Idempotency-Key is too long");
        }
        return normalized;
    }

    private String askPath(UUID libraryId, UUID conversationId) {
        return "/libraries/" + libraryId + "/conversations/" + conversationId + "/messages";
    }

    private String requestHash(AssistantDtos.AskRequest request) {
        String referenced = request.searchScope() == null || request.searchScope().referencedLibraryIds() == null
                ? "" : request.searchScope().referencedLibraryIds().stream().map(UUID::toString).sorted()
                .reduce((left, right) -> left + "," + right).orElse("");
        boolean current = request.searchScope() == null || request.searchScope().currentLibrary() == null
                || request.searchScope().currentLibrary();
        String canonical = String.join("\u001f", value(request.content(), ""), value(request.answerMode(), ""),
                value(request.processingMode(), ""), Boolean.toString(current), referenced);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String normalizeRating(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        if ("LIKE".equals(normalized) || "UP".equals(normalized)) {
            normalized = "HELPFUL";
        } else if ("DISLIKE".equals(normalized) || "DOWN".equals(normalized)) {
            normalized = "NOT_HELPFUL";
        }
        if (!RATINGS.contains(normalized)) {
            throw badRequest("rating must be HELPFUL or NOT_HELPFUL");
        }
        return normalized;
    }

    private ConversationEntity requireConversation(UUID ownerId, UUID libraryId, UUID conversationId) {
        requireLibrary(ownerId, libraryId);
        return conversationRepository.findByIdAndLibraryIdAndUserId(conversationId, libraryId, ownerId)
                .orElseThrow(() -> notFound("conversation not found"));
    }

    private LibraryEntity requireLibrary(UUID ownerId, UUID libraryId) {
        return libraryRepository.findByIdAndOwnerId(libraryId, ownerId)
                .orElseThrow(() -> notFound("library not found"));
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ASSISTANT_REQUEST", message);
    }

    private ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    private record CitationDraft(SearchDtos.SearchResult result, EvidenceEntity evidence) {
    }
}
