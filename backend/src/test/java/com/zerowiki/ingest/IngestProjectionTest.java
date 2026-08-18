package com.zerowiki.ingest;

import com.zerowiki.knowledge.ChangeItemEntity;
import com.zerowiki.knowledge.ChangeItemRepository;
import com.zerowiki.knowledge.ChangeSetRepository;
import com.zerowiki.knowledge.ClaimRepository;
import com.zerowiki.knowledge.ClaimStatusRepository;
import com.zerowiki.knowledge.EvidenceRepository;
import com.zerowiki.knowledge.PageEntity;
import com.zerowiki.knowledge.PageRelationEntity;
import com.zerowiki.knowledge.PageRelationRepository;
import com.zerowiki.knowledge.PageRepository;
import com.zerowiki.knowledge.PageVersionEntity;
import com.zerowiki.knowledge.PageVersionRepository;
import com.zerowiki.library.LibraryRepository;
import com.zerowiki.source.SourceEntity;
import com.zerowiki.source.SourceService;
import com.zerowiki.source.SourceType;
import com.zerowiki.source.SourceVersionEntity;
import com.zerowiki.source.CaptureMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestProjectionTest {
    @Mock IngestJobRepository jobRepository;
    @Mock IngestItemRepository itemRepository;
    @Mock JobQuestionRepository questionRepository;
    @Mock LibraryRepository libraryRepository;
    @Mock SourceService sourceService;
    @Mock PageRepository pageRepository;
    @Mock PageVersionRepository pageVersionRepository;
    @Mock ClaimRepository claimRepository;
    @Mock ClaimStatusRepository claimStatusRepository;
    @Mock EvidenceRepository evidenceRepository;
    @Mock PageRelationRepository relationRepository;
    @Mock ChangeSetRepository changeSetRepository;
    @Mock ChangeItemRepository changeItemRepository;

    @Test
    void process_one_projects_source_concept_claim_evidence_relation_and_review() {
        UUID owner = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        IngestJobEntity job = IngestJobEntity.create(libraryId, owner, ImportType.OBSIDIAN_ZIP,
                ProcessingMode.STANDARD, new ObjectMapper().createObjectNode(), 10, 1);
        job.moveToPlanReview(job.getPlan(), job.getEstimatedTokens());
        job.approvePlan();
        SourceEntity source = SourceEntity.create(owner, SourceType.FILE, "file://raft.md", "Raft");
        SourceVersionEntity sourceVersion = SourceVersionEntity.create(source.getId(), 1, "a".repeat(64),
                "raft.md", "text/markdown", 18, new ObjectMapper().createObjectNode(), CaptureMethod.UPLOAD);
        IngestItemEntity item = IngestItemEntity.create(job.getId(), sourceVersion.getId(),
                new ObjectMapper().createArrayNode());
        Map<UUID, PageVersionEntity> versions = new HashMap<>();
        Map<String, PageEntity> pages = new HashMap<>();
        AtomicReference<PageRelationEntity> relation = new AtomicReference<>();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(itemRepository.findAllByIngestJobIdAndStatus(job.getId(), IngestItemStatus.PENDING))
                .thenReturn(List.of(item));
        when(itemRepository.findAllByIngestJobIdOrderByCreatedAtAsc(job.getId())).thenReturn(List.of(item));
        when(itemRepository.save(any(IngestItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(IngestJobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sourceService.versionById(owner, sourceVersion.getId())).thenReturn(sourceVersion);
        when(sourceService.get(owner, source.getId())).thenReturn(source);
        when(sourceService.open(sourceVersion)).thenReturn(new ByteArrayInputStream("Raft leader election".getBytes()));
        when(pageRepository.findByLibraryIdAndSlug(any(), any())).thenAnswer(invocation ->
                Optional.ofNullable(pages.get(invocation.getArgument(1))));
        when(pageRepository.save(any())).thenAnswer(invocation -> {
            PageEntity value = invocation.getArgument(0);
            pages.put(value.getSlug(), value);
            return value;
        });
        when(pageVersionRepository.findTopByPageIdOrderByVersionNoDesc(any())).thenAnswer(invocation -> {
            PageVersionEntity value = versions.get(invocation.getArgument(0));
            return Optional.ofNullable(value);
        });
        when(pageVersionRepository.save(any())).thenAnswer(invocation -> {
            PageVersionEntity value = invocation.getArgument(0);
            versions.put(value.getPageId(), value);
            return value;
        });
        when(claimRepository.findByPageIdAndClaimKey(any(), any())).thenReturn(Optional.empty());
        when(claimRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(claimStatusRepository.findByClaimIdAndPageVersionId(any(), any())).thenReturn(Optional.empty());
        when(claimStatusRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenceRepository.findByClaimIdAndSourceVersionId(any(), any())).thenReturn(Optional.empty());
        when(evidenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(relationRepository.findByLibraryIdAndSourcePageIdAndTargetPageIdAndRelationType(any(), any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(relation.get()));
        when(relationRepository.save(any())).thenAnswer(invocation -> {
            PageRelationEntity value = invocation.getArgument(0);
            relation.set(value);
            return value;
        });
        when(changeSetRepository.findByLibraryIdAndOriginTypeAndOriginId(any(), any(), any())).thenReturn(Optional.empty());
        when(changeSetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IngestService service = new IngestService(jobRepository, itemRepository, questionRepository, libraryRepository,
                sourceService, new ObjectMapper(), pageRepository, pageVersionRepository, claimRepository,
                claimStatusRepository, evidenceRepository, relationRepository, changeSetRepository, changeItemRepository);

        assertThat(service.processOne(job.getId())).isTrue();
        assertThat(item.getStatus()).isEqualTo(IngestItemStatus.DONE);
        assertThat(job.getStatus()).isEqualTo(IngestStatus.CHANGE_REVIEW);
        org.mockito.Mockito.verify(claimRepository).save(any());
        org.mockito.Mockito.verify(evidenceRepository).save(any());
        org.mockito.ArgumentCaptor<PageEntity> pageCaptor = org.mockito.ArgumentCaptor.forClass(PageEntity.class);
        org.mockito.Mockito.verify(pageRepository, org.mockito.Mockito.times(4)).save(pageCaptor.capture());
        assertThat(pageCaptor.getAllValues()).allMatch(page -> page.getStatus().equals("DRAFT"));
        org.mockito.ArgumentCaptor<PageRelationEntity> relationCaptor =
                org.mockito.ArgumentCaptor.forClass(PageRelationEntity.class);
        org.mockito.Mockito.verify(relationRepository).save(relationCaptor.capture());
        assertThat(relationCaptor.getValue().getStatus()).isEqualTo("PROPOSED");
        org.mockito.ArgumentCaptor<ChangeItemEntity> itemCaptor =
                org.mockito.ArgumentCaptor.forClass(ChangeItemEntity.class);
        org.mockito.Mockito.verify(changeItemRepository, org.mockito.Mockito.times(3)).save(itemCaptor.capture());
        assertThat(itemCaptor.getAllValues()).anyMatch(change -> "PUBLISHED"
                .equals(change.getAfterSnapshot().path("status").asText()));
        assertThat(itemCaptor.getAllValues()).anyMatch(change -> "ACCEPTED"
                .equals(change.getAfterSnapshot().path("status").asText()));
        org.mockito.Mockito.verify(changeSetRepository).save(any());
    }
}
