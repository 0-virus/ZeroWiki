package com.zerowiki.knowledge;

import com.zerowiki.common.ApiException;
import com.zerowiki.library.LibraryEntity;
import com.zerowiki.library.LibraryRepository;
import com.zerowiki.source.SourceRepository;
import com.zerowiki.source.SourceVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {
    @Mock LibraryRepository libraryRepository;
    @Mock PageRepository pageRepository;
    @Mock PageVersionRepository pageVersionRepository;
    @Mock ClaimRepository claimRepository;
    @Mock ClaimStatusRepository claimStatusRepository;
    @Mock EvidenceRepository evidenceRepository;
    @Mock PageRelationRepository relationRepository;
    @Mock ContradictionRepository contradictionRepository;
    @Mock ChangeSetRepository changeSetRepository;
    @Mock ChangeItemRepository changeItemRepository;
    @Mock SourceRepository sourceRepository;
    @Mock SourceVersionRepository sourceVersionRepository;

    private KnowledgeService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeService(libraryRepository, pageRepository, pageVersionRepository, claimRepository,
                claimStatusRepository, evidenceRepository, relationRepository, contradictionRepository,
                changeSetRepository, changeItemRepository, sourceRepository, sourceVersionRepository,
                new ObjectMapper());
    }

    @Test
    void every_knowledge_read_requires_owner_library_boundary() {
        UUID owner = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        when(libraryRepository.findByIdAndOwnerId(libraryId, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pages(owner, libraryId, null, null, null, null, 20))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus().value()).isEqualTo(404));
        verify(pageRepository, never()).findAllByLibraryIdOrderByUpdatedAtDesc(libraryId);
    }

    @Test
    void resolution_creates_change_set_without_mutating_contradiction() {
        UUID owner = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        UUID contradictionId = UUID.randomUUID();
        UUID leftId = UUID.randomUUID();
        UUID rightId = UUID.randomUUID();
        UUID pageId = UUID.randomUUID();
        LibraryEntity library = org.mockito.Mockito.mock(LibraryEntity.class);
        ContradictionEntity contradiction = org.mockito.Mockito.mock(ContradictionEntity.class);
        ClaimEntity left = org.mockito.Mockito.mock(ClaimEntity.class);
        ClaimEntity right = org.mockito.Mockito.mock(ClaimEntity.class);
        PageEntity page = org.mockito.Mockito.mock(PageEntity.class);
        ChangeSetEntity changeSet = org.mockito.Mockito.mock(ChangeSetEntity.class);

        when(libraryRepository.findByIdAndOwnerId(libraryId, owner)).thenReturn(Optional.of(library));
        when(contradictionRepository.findByIdAndLibraryId(contradictionId, libraryId))
                .thenReturn(Optional.of(contradiction));
        when(contradiction.getLeftClaimId()).thenReturn(leftId);
        when(contradiction.getRightClaimId()).thenReturn(rightId);
        when(claimRepository.findById(leftId)).thenReturn(Optional.of(left));
        when(claimRepository.findById(rightId)).thenReturn(Optional.of(right));
        when(left.getPageId()).thenReturn(pageId);
        when(right.getPageId()).thenReturn(pageId);
        when(pageRepository.findByIdAndLibraryId(pageId, libraryId)).thenReturn(Optional.of(page));
        when(changeSetRepository.save(any(ChangeSetEntity.class))).thenReturn(changeSet);
        when(changeSet.getId()).thenReturn(UUID.randomUUID());
        when(changeSet.getStatus()).thenReturn("READY_FOR_REVIEW");
        when(changeSet.getRiskLevel()).thenReturn("MAJOR");

        KnowledgeDtos.ChangeSetResponse response = service.resolutionProposal(owner, libraryId, contradictionId,
                new KnowledgeDtos.ResolutionRequest("CLASSIFY", "TIME_CHANGE", "review"));

        assertThat(response.status()).isEqualTo("READY_FOR_REVIEW");
        verify(changeSetRepository).save(any(ChangeSetEntity.class));
        verify(changeItemRepository).save(any(ChangeItemEntity.class));
        verify(contradictionRepository, never()).save(any(ContradictionEntity.class));
    }
}
