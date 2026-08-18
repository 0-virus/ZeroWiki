package com.zerowiki.assistant;

import com.zerowiki.knowledge.ChangeItemRepository;
import com.zerowiki.knowledge.ChangeSetRepository;
import com.zerowiki.knowledge.EvidenceEntity;
import com.zerowiki.knowledge.EvidenceRepository;
import com.zerowiki.knowledge.PageVersionRepository;
import com.zerowiki.library.LibraryEntity;
import com.zerowiki.library.LibraryRepository;
import com.zerowiki.library.TemplateType;
import com.zerowiki.search.SearchDtos;
import com.zerowiki.search.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {
    @Mock LibraryRepository libraryRepository;
    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock MessageCitationRepository citationRepository;
    @Mock FeedbackEventRepository feedbackRepository;
    @Mock SearchService searchService;
    @Mock ChangeSetRepository changeSetRepository;
    @Mock ChangeItemRepository changeItemRepository;
    @Mock PageVersionRepository pageVersionRepository;
    @Mock EvidenceRepository evidenceRepository;
    @Mock IdempotencyRecordRepository idempotencyRepository;

    @Test
    void answer_without_traceable_evidence_is_explicitly_insufficient() {
        UUID owner = UUID.randomUUID();
        LibraryEntity library = LibraryEntity.create(owner, "Library", null, TemplateType.CUSTOM);
        ConversationEntity conversation = ConversationEntity.create(library.getId(), owner, "Question");
        when(libraryRepository.findByIdAndOwnerId(library.getId(), owner)).thenReturn(Optional.of(library));
        when(conversationRepository.findByIdAndLibraryIdAndUserId(conversation.getId(), library.getId(), owner))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(searchService.results(any(), any(), any(), any(), any(), any(Integer.class), any()))
                .thenReturn(List.of());

        AssistantService service = new AssistantService(libraryRepository, conversationRepository, messageRepository,
                citationRepository, feedbackRepository, searchService, changeSetRepository, changeItemRepository,
                pageVersionRepository, evidenceRepository, idempotencyRepository, new ObjectMapper());

        AssistantDtos.QueuedResponse response = service.ask(owner, library.getId(), conversation.getId(),
                new AssistantDtos.AskRequest("What is unknown?", "QUICK", "STANDARD",
                        new AssistantDtos.SearchScope(true, List.of())));

        assertThat(response.status()).isEqualTo("QUEUED");
        org.mockito.Mockito.verify(messageRepository, org.mockito.Mockito.times(2)).save(any(MessageEntity.class));
    }

    @Test
    void idempotent_ask_reuses_response_and_rejects_different_body() {
        UUID owner = UUID.randomUUID();
        LibraryEntity library = LibraryEntity.create(owner, "Library", null, TemplateType.CUSTOM);
        ConversationEntity conversation = ConversationEntity.create(library.getId(), owner, "Question");
        when(libraryRepository.findByIdAndOwnerId(library.getId(), owner)).thenReturn(Optional.of(library));
        when(conversationRepository.findByIdAndLibraryIdAndUserId(conversation.getId(), library.getId(), owner))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(searchService.results(any(), any(), any(), any(), any(), any(Integer.class), any()))
                .thenReturn(List.of());
        AtomicReference<IdempotencyRecordEntity> stored = new AtomicReference<>();
        when(idempotencyRepository.findByUserIdAndRequestPathAndIdempotencyKey(any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(idempotencyRepository.save(any(IdempotencyRecordEntity.class))).thenAnswer(invocation -> {
            IdempotencyRecordEntity value = invocation.getArgument(0);
            stored.set(value);
            return value;
        });

        AssistantService service = new AssistantService(libraryRepository, conversationRepository, messageRepository,
                citationRepository, feedbackRepository, searchService, changeSetRepository, changeItemRepository,
                pageVersionRepository, evidenceRepository, idempotencyRepository, new ObjectMapper());
        AssistantDtos.AskRequest request = new AssistantDtos.AskRequest("same question", "QUICK", "STANDARD",
                new AssistantDtos.SearchScope(true, List.of()));

        AssistantDtos.QueuedResponse first = service.ask(owner, library.getId(), conversation.getId(), request, "key-1");
        AssistantDtos.QueuedResponse second = service.ask(owner, library.getId(), conversation.getId(), request, "key-1");

        assertThat(second.userMessageId()).isEqualTo(first.userMessageId());
        assertThat(second.assistantMessageId()).isEqualTo(first.assistantMessageId());
        org.mockito.Mockito.verify(messageRepository, org.mockito.Mockito.times(2)).save(any(MessageEntity.class));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.ask(owner, library.getId(), conversation.getId(),
                        new AssistantDtos.AskRequest("different question", "QUICK", "STANDARD",
                                new AssistantDtos.SearchScope(true, List.of())), "key-1"))
                .isInstanceOf(com.zerowiki.common.ApiException.class)
                .satisfies(error -> {
                    com.zerowiki.common.ApiException api = (com.zerowiki.common.ApiException) error;
                    assertThat(api.getStatus().value()).isEqualTo(409);
                    assertThat(api.getCode()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
                });
    }

    @Test
    void answer_searches_claims_only_so_citation_survives_page_candidates() {
        UUID owner = UUID.randomUUID();
        LibraryEntity library = LibraryEntity.create(owner, "Library", null, TemplateType.CUSTOM);
        ConversationEntity conversation = ConversationEntity.create(library.getId(), owner, "Question");
        UUID pageVersionId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        UUID sourceVersionId = UUID.randomUUID();
        ObjectMapper mapper = new ObjectMapper();
        SearchDtos.SearchResult claimResult = new SearchDtos.SearchResult("CLAIM", "Phase 1 claim",
                "phase1 evidence", 1.0, UUID.randomUUID(), pageVersionId, null, claimId, sourceVersionId,
                mapper.createObjectNode().put("line", 1),
                new SearchDtos.Origin("CURRENT", library.getId(), library.getName()));
        EvidenceEntity evidence = EvidenceEntity.create(claimId, sourceVersionId, "SUPPORTS",
                mapper.createObjectNode().put("line", 1), "phase1 evidence", "en", BigDecimal.ONE);

        when(libraryRepository.findByIdAndOwnerId(library.getId(), owner)).thenReturn(Optional.of(library));
        when(conversationRepository.findByIdAndLibraryIdAndUserId(conversation.getId(), library.getId(), owner))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(searchService.results(owner, library.getId(), "Explain phase1 and cite it", Set.of("CLAIM"),
                "KEYWORD", 8, "CURRENT")).thenReturn(List.of(claimResult));
        when(evidenceRepository.findByClaimIdAndSourceVersionId(claimId, sourceVersionId))
                .thenReturn(Optional.of(evidence));

        AssistantService service = new AssistantService(libraryRepository, conversationRepository, messageRepository,
                citationRepository, feedbackRepository, searchService, changeSetRepository, changeItemRepository,
                pageVersionRepository, evidenceRepository, idempotencyRepository, mapper);

        AssistantDtos.QueuedResponse response = service.ask(owner, library.getId(), conversation.getId(),
                new AssistantDtos.AskRequest("Explain phase1 and cite it", "QUICK", "STANDARD",
                        new AssistantDtos.SearchScope(true, List.of())));

        assertThat(response.status()).isEqualTo("QUEUED");
        verify(searchService).results(owner, library.getId(), "Explain phase1 and cite it", Set.of("CLAIM"),
                "KEYWORD", 8, "CURRENT");
        verify(citationRepository).save(any(MessageCitationEntity.class));
    }
}
