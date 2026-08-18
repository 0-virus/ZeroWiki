package com.zerowiki.knowledge;

import com.zerowiki.common.ApiException;
import com.zerowiki.ingest.IngestJobEntity;
import com.zerowiki.ingest.IngestJobRepository;
import com.zerowiki.ingest.ImportType;
import com.zerowiki.ingest.IngestStatus;
import com.zerowiki.ingest.ProcessingMode;
import com.zerowiki.library.LibraryEntity;
import com.zerowiki.library.LibraryRepository;
import com.zerowiki.library.TemplateType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeSetServiceTest {
    @Mock LibraryRepository libraryRepository;
    @Mock ChangeSetRepository changeSetRepository;
    @Mock ChangeItemRepository changeItemRepository;
    @Mock ChangeReviewRepository changeReviewRepository;
    @Mock LibraryVersionRepository libraryVersionRepository;
    @Mock PageRepository pageRepository;
    @Mock PageVersionRepository pageVersionRepository;
    @Mock PageRelationRepository relationRepository;
    @Mock ContradictionRepository contradictionRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock IngestJobRepository ingestJobRepository;

    @Test
    void review_enforces_owner_and_comment_contract() {
        UUID owner = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        UUID changeSetId = UUID.randomUUID();
        ChangeSetService service = service();
        when(libraryRepository.findByIdAndOwnerId(libraryId, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.review(owner, libraryId, changeSetId,
                new ChangeSetDtos.ReviewRequest(1L,
                        List.of(new ChangeSetDtos.ReviewDecision(UUID.randomUUID(), "REJECT", "reason")))))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus().value()).isEqualTo(404));
    }

    @Test
    void request_changes_keeps_item_identity_and_marks_revision_requested() {
        UUID owner = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        LibraryEntity library = LibraryEntity.create(owner, "Library", null, TemplateType.CUSTOM);
        ChangeSetEntity changeSet = ChangeSetEntity.ready(libraryId, "USER_EDIT", UUID.randomUUID(),
                "Title", "Summary", "MINOR");
        ChangeItemEntity item = ChangeItemEntity.update(changeSet.getId(), "PAGE", UUID.randomUUID(),
                new ObjectMapper().createObjectNode(), new ObjectMapper().createObjectNode());
        ChangeSetService service = service();
        when(libraryRepository.findByIdAndOwnerId(libraryId, owner)).thenReturn(Optional.of(library));
        when(changeSetRepository.findByIdAndLibraryId(changeSet.getId(), libraryId)).thenReturn(Optional.of(changeSet));
        when(changeItemRepository.findByIdAndChangeSetId(item.getId(), changeSet.getId())).thenReturn(Optional.of(item));
        when(changeItemRepository.findAllByChangeSetIdOrderByCreatedAtAsc(changeSet.getId())).thenReturn(List.of(item));

        ChangeSetService.ReviewResult result = service.review(owner, libraryId, changeSet.getId(),
                new ChangeSetDtos.ReviewRequest(changeSet.getRevision(),
                        List.of(new ChangeSetDtos.ReviewDecision(item.getId(), "REQUEST_CHANGES", "add evidence"))));

        assertThat(result.accepted()).isTrue();
        assertThat(result.response().id()).isEqualTo(changeSet.getId());
        assertThat(item.getReviewStatus()).isEqualTo("REVISION_REQUESTED");
        assertThat(changeSet.getStatus()).isEqualTo("PARTIALLY_APPROVED");
        verify(changeReviewRepository).save(any(ChangeReviewEntity.class));
    }

    @Test
    void reject_without_comment_is_rejected_before_item_mutation() {
        UUID owner = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        LibraryEntity library = LibraryEntity.create(owner, "Library", null, TemplateType.CUSTOM);
        ChangeSetEntity changeSet = ChangeSetEntity.ready(libraryId, "USER_EDIT", UUID.randomUUID(),
                "Title", "Summary", "MINOR");
        ChangeSetService service = service();
        when(libraryRepository.findByIdAndOwnerId(libraryId, owner)).thenReturn(Optional.of(library));
        when(changeSetRepository.findByIdAndLibraryId(changeSet.getId(), libraryId)).thenReturn(Optional.of(changeSet));

        assertThatThrownBy(() -> service.review(owner, libraryId, changeSet.getId(),
                new ChangeSetDtos.ReviewRequest(changeSet.getRevision(),
                        List.of(new ChangeSetDtos.ReviewDecision(UUID.randomUUID(), "REJECT", "  ")))))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus().value()).isEqualTo(400));
    }

    @Test
    void approved_archive_relation_and_contradiction_items_are_applied() {
        UUID owner = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        LibraryEntity library = LibraryEntity.create(owner, "Library", null, TemplateType.CUSTOM);
        ChangeSetEntity changeSet = ChangeSetEntity.ready(libraryId, "USER_EDIT", UUID.randomUUID(),
                "Title", "Summary", "MAJOR");
        UUID pageId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();
        UUID contradictionId = UUID.randomUUID();
        tools.jackson.databind.node.ObjectNode archive = new ObjectMapper().createObjectNode();
        archive.put("status", "ARCHIVED");
        tools.jackson.databind.node.ObjectNode feedback = new ObjectMapper().createObjectNode();
        feedback.put("feedbackType", "IRRELEVANT");
        tools.jackson.databind.node.ObjectNode resolution = new ObjectMapper().createObjectNode();
        resolution.put("decision", "CLASSIFY");
        resolution.put("classification", "TIME_CHANGE");
        resolution.put("resolutionNote", "version differs");
        ChangeItemEntity pageItem = ChangeItemEntity.update(changeSet.getId(), "PAGE", pageId, null, archive);
        ChangeItemEntity relationItem = ChangeItemEntity.update(changeSet.getId(), "RELATION", relationId, null, feedback);
        ChangeItemEntity contradictionItem = ChangeItemEntity.update(changeSet.getId(), "CONTRADICTION",
                contradictionId, null, resolution);
        List<ChangeItemEntity> items = List.of(pageItem, relationItem, contradictionItem);
        items.forEach(item -> item.review("APPROVE"));
        changeSet.refreshReviewStatus(items);

        PageEntity page = org.mockito.Mockito.mock(PageEntity.class);
        PageRelationEntity relation = org.mockito.Mockito.mock(PageRelationEntity.class);
        ContradictionEntity contradiction = org.mockito.Mockito.mock(ContradictionEntity.class);
        when(pageRepository.findByIdAndLibraryId(pageId, libraryId)).thenReturn(Optional.of(page));
        when(relationRepository.findById(relationId)).thenReturn(Optional.of(relation));
        when(relation.getLibraryId()).thenReturn(libraryId);
        when(contradictionRepository.findByIdAndLibraryId(contradictionId, libraryId))
                .thenReturn(Optional.of(contradiction));
        when(changeSetRepository.findByIdAndLibraryId(changeSet.getId(), libraryId)).thenReturn(Optional.of(changeSet));
        when(libraryRepository.findByIdAndOwnerId(libraryId, owner)).thenReturn(Optional.of(library));
        when(changeItemRepository.findAllByChangeSetIdOrderByCreatedAtAsc(changeSet.getId())).thenReturn(items);
        when(libraryVersionRepository.findTopByLibraryIdOrderByVersionNoDesc(libraryId)).thenReturn(Optional.empty());
        when(libraryVersionRepository.save(any(LibraryVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangeSetDtos.ApplyResponse response = service().apply(owner, libraryId, changeSet.getId(),
                new ChangeSetDtos.ApplyRequest(changeSet.getRevision(), 0L, null));

        assertThat(response.status()).isEqualTo("APPLIED");
        org.mockito.Mockito.verify(page).applyStatus("ARCHIVED");
        org.mockito.Mockito.verify(relation).reject();
        org.mockito.Mockito.verify(contradiction).resolve("RESOLVED", "TIME_CHANGE", "version differs", owner);
        verify(pageRepository).save(page);
        verify(relationRepository).save(relation);
        verify(contradictionRepository).save(contradiction);
        verify(auditLogRepository, org.mockito.Mockito.times(3)).save(any(AuditLogEntity.class));
    }

    @Test
    void approved_change_set_creates_new_library_version_without_auto_mutating_items() {
        UUID owner = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        LibraryEntity library = LibraryEntity.create(owner, "Library", null, TemplateType.CUSTOM);
        ChangeSetEntity changeSet = ChangeSetEntity.ready(libraryId, "USER_EDIT", UUID.randomUUID(),
                "Title", "Summary", "MINOR");
        UUID pageId = UUID.randomUUID();
        tools.jackson.databind.node.ObjectNode after = new ObjectMapper().createObjectNode();
        after.put("baseVersionNo", 1);
        after.put("proposedMarkdown", "new body");
        ChangeItemEntity item = ChangeItemEntity.update(changeSet.getId(), "PAGE", pageId,
                new ObjectMapper().createObjectNode(), after);
        item.review("APPROVE");
        ChangeItemEntity rejected = ChangeItemEntity.update(changeSet.getId(), "PAGE", UUID.randomUUID(),
                null, after);
        rejected.review("REJECT");
        changeSet.refreshReviewStatus(List.of(item, rejected));
        PageEntity page = org.mockito.Mockito.mock(PageEntity.class);
        when(page.getId()).thenReturn(pageId);
        PageVersionEntity current = PageVersionEntity.create(pageId, 1, "Title", "old body", null, null, owner);
        ChangeSetService service = service();
        when(libraryRepository.findByIdAndOwnerId(libraryId, owner)).thenReturn(Optional.of(library));
        when(changeSetRepository.findByIdAndLibraryId(changeSet.getId(), libraryId)).thenReturn(Optional.of(changeSet));
        when(changeItemRepository.findAllByChangeSetIdOrderByCreatedAtAsc(changeSet.getId()))
                .thenReturn(List.of(item, rejected));
        when(pageRepository.findByIdAndLibraryId(pageId, libraryId)).thenReturn(Optional.of(page));
        when(pageVersionRepository.findTopByPageIdOrderByVersionNoDesc(pageId)).thenReturn(Optional.of(current));
        when(pageVersionRepository.save(any(PageVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(libraryVersionRepository.findTopByLibraryIdOrderByVersionNoDesc(libraryId)).thenReturn(Optional.empty());
        when(libraryVersionRepository.save(any(LibraryVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangeSetDtos.ApplyResponse response = service.apply(owner, libraryId, changeSet.getId(),
                new ChangeSetDtos.ApplyRequest(changeSet.getRevision(), 0L, "Published"));

        assertThat(response.status()).isEqualTo("APPLIED");
        assertThat(library.getCurrentLibraryVersionId()).isNotNull();
        assertThat(item.getReviewStatus()).isEqualTo("APPROVED");
        assertThat(rejected.getReviewStatus()).isEqualTo("REJECTED");
        verify(pageVersionRepository).save(any(PageVersionEntity.class));
        verify(pageRepository).save(page);
        verify(auditLogRepository).save(any(AuditLogEntity.class));
        verify(libraryVersionRepository).save(any(LibraryVersionEntity.class));
    }

    @Test
    void approved_create_page_item_materializes_new_page_and_initial_version() {
        UUID owner = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        LibraryEntity library = LibraryEntity.create(owner, "Library", null, TemplateType.CUSTOM);
        ChangeSetEntity changeSet = ChangeSetEntity.ready(libraryId, "ASSISTANT", UUID.randomUUID(),
                "Assistant proposal", "Create a synthesis page", "MINOR");
        UUID proposedPageId = UUID.randomUUID();
        tools.jackson.databind.node.ObjectNode after = new ObjectMapper().createObjectNode();
        after.put("pageType", "SYNTHESIS");
        after.put("status", "PUBLISHED");
        after.put("title", "Raft와 Paxos 비교");
        after.put("baseVersionNo", 0);
        after.put("proposedMarkdown", "# Raft와 Paxos 비교\n\n근거 기반 요약");
        ChangeItemEntity item = ChangeItemEntity.create(changeSet.getId(), "PAGE", proposedPageId, after);
        item.review("APPROVE");
        changeSet.refreshReviewStatus(List.of(item));

        when(libraryRepository.findByIdAndOwnerId(libraryId, owner)).thenReturn(Optional.of(library));
        when(changeSetRepository.findByIdAndLibraryId(changeSet.getId(), libraryId)).thenReturn(Optional.of(changeSet));
        when(changeItemRepository.findAllByChangeSetIdOrderByCreatedAtAsc(changeSet.getId()))
                .thenReturn(List.of(item));
        when(pageRepository.findByIdAndLibraryId(proposedPageId, libraryId)).thenReturn(Optional.empty());
        when(pageRepository.save(any(PageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pageVersionRepository.save(any(PageVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(libraryVersionRepository.findTopByLibraryIdOrderByVersionNoDesc(libraryId)).thenReturn(Optional.empty());
        when(libraryVersionRepository.save(any(LibraryVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangeSetDtos.ApplyResponse response = service().apply(owner, libraryId, changeSet.getId(),
                new ChangeSetDtos.ApplyRequest(changeSet.getRevision(), 0L, "Assistant proposal"));

        assertThat(response.status()).isEqualTo("APPLIED");
        org.mockito.ArgumentCaptor<PageEntity> pages = org.mockito.ArgumentCaptor.forClass(PageEntity.class);
        verify(pageRepository, org.mockito.Mockito.times(2)).save(pages.capture());
        assertThat(pages.getAllValues().getFirst().getPageType()).isEqualTo("SYNTHESIS");
        assertThat(pages.getAllValues().getFirst().getStatus()).isEqualTo("PUBLISHED");
        verify(pageVersionRepository).save(any(PageVersionEntity.class));
    }

    @Test
    void approved_ingest_snapshot_publishes_page_and_accepts_relation() {
        UUID owner = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        LibraryEntity library = LibraryEntity.create(owner, "Library", null, TemplateType.CUSTOM);
        IngestJobEntity ingestJob = IngestJobEntity.create(libraryId, owner, ImportType.OBSIDIAN_ZIP,
                ProcessingMode.STANDARD, new ObjectMapper().createObjectNode(), 0, 1);
        ingestJob.moveToPlanReview(ingestJob.getPlan(), ingestJob.getEstimatedTokens());
        ingestJob.approvePlan();
        ingestJob.moveToChangeReview();
        ChangeSetEntity changeSet = ChangeSetEntity.ready(libraryId, "INGEST", ingestJob.getId(),
                "Import", "Publish projection", "MINOR");
        PageEntity source = PageEntity.create(libraryId, "source", "SOURCE", "DRAFT");
        PageEntity concept = PageEntity.create(libraryId, "concept", "CONCEPT", "DRAFT");
        PageRelationEntity relation = PageRelationEntity.proposed(libraryId, source.getId(), concept.getId(),
                "EXPLAINS", "projection", java.math.BigDecimal.valueOf(0.8));
        tools.jackson.databind.node.ObjectNode publish = new ObjectMapper().createObjectNode();
        publish.put("status", "PUBLISHED");
        tools.jackson.databind.node.ObjectNode accept = new ObjectMapper().createObjectNode();
        accept.put("status", "ACCEPTED");
        ChangeItemEntity pageItem = ChangeItemEntity.update(changeSet.getId(), "PAGE", concept.getId(), null, publish);
        ChangeItemEntity relationItem = ChangeItemEntity.update(changeSet.getId(), "RELATION", relation.getId(), null, accept);
        List<ChangeItemEntity> items = List.of(pageItem, relationItem);
        items.forEach(item -> item.review("APPROVE"));
        changeSet.refreshReviewStatus(items);

        when(libraryRepository.findByIdAndOwnerId(libraryId, owner)).thenReturn(Optional.of(library));
        when(changeSetRepository.findByIdAndLibraryId(changeSet.getId(), libraryId)).thenReturn(Optional.of(changeSet));
        when(changeItemRepository.findAllByChangeSetIdOrderByCreatedAtAsc(changeSet.getId())).thenReturn(items);
        when(pageRepository.findByIdAndLibraryId(concept.getId(), libraryId)).thenReturn(Optional.of(concept));
        when(relationRepository.findById(relation.getId())).thenReturn(Optional.of(relation));
        when(ingestJobRepository.findByIdAndLibraryIdAndRequestedBy(ingestJob.getId(), libraryId, owner))
                .thenReturn(Optional.of(ingestJob));
        when(ingestJobRepository.save(any(IngestJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(libraryVersionRepository.findTopByLibraryIdOrderByVersionNoDesc(libraryId)).thenReturn(Optional.empty());
        when(libraryVersionRepository.save(any(LibraryVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangeSetDtos.ApplyResponse response = service().apply(owner, libraryId, changeSet.getId(),
                new ChangeSetDtos.ApplyRequest(changeSet.getRevision(), 0L, "Import"));

        assertThat(response.status()).isEqualTo("APPLIED");
        assertThat(concept.getStatus()).isEqualTo("PUBLISHED");
        assertThat(relation.getStatus()).isEqualTo("ACCEPTED");
        assertThat(ingestJob.getStatus()).isEqualTo(IngestStatus.COMPLETED);
        verify(pageRepository).save(concept);
        verify(relationRepository).save(relation);
        verify(ingestJobRepository).save(ingestJob);
    }

    private ChangeSetService service() {
        return new ChangeSetService(libraryRepository, changeSetRepository, changeItemRepository,
                changeReviewRepository, libraryVersionRepository, pageRepository, pageVersionRepository,
                relationRepository, contradictionRepository, auditLogRepository, ingestJobRepository,
                new ObjectMapper());
    }
}
