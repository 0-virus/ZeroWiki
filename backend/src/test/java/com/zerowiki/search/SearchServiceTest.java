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
import com.zerowiki.source.LibrarySourceRepository;
import com.zerowiki.source.CaptureMethod;
import com.zerowiki.source.SourceRepository;
import com.zerowiki.source.SourceEntity;
import com.zerowiki.source.SourceType;
import com.zerowiki.source.SourceVersionEntity;
import com.zerowiki.source.SourceVersionRepository;
import com.zerowiki.library.TemplateType;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {
    @Mock LibraryRepository libraryRepository;
    @Mock LibraryReferenceRepository referenceRepository;
    @Mock LibrarySourceRepository librarySourceRepository;
    @Mock SourceRepository sourceRepository;
    @Mock SourceVersionRepository sourceVersionRepository;
    @Mock PageRepository pageRepository;
    @Mock PageVersionRepository pageVersionRepository;
    @Mock ClaimRepository claimRepository;
    @Mock EvidenceRepository evidenceRepository;

    private SearchService service;

    @BeforeEach
    void setUp() {
        service = new SearchService(libraryRepository, referenceRepository, librarySourceRepository, sourceRepository,
                sourceVersionRepository, pageRepository, pageVersionRepository, claimRepository, evidenceRepository);
    }

    @Test
    void every_mode_uses_deterministic_keyword_fallback_with_owner_boundary() {
        UUID owner = UUID.randomUUID();
        LibraryEntity library = LibraryEntity.create(owner, "Current", null, TemplateType.CUSTOM);
        PageEntity page = PageEntity.create(library.getId(), "raft", "CONCEPT", "PUBLISHED");
        PageVersionEntity version = PageVersionEntity.imported(page.getId(), 1, "Raft", "Raft leader election", null, null);
        when(libraryRepository.findByIdAndOwnerId(library.getId(), owner)).thenReturn(Optional.of(library));
        when(pageRepository.findAllByLibraryIdOrderByUpdatedAtDesc(library.getId())).thenReturn(List.of(page));
        when(pageVersionRepository.findTopByPageIdOrderByVersionNoDesc(page.getId())).thenReturn(Optional.of(version));

        for (String mode : List.of("KEYWORD", "SEMANTIC", "HYBRID")) {
            SearchDtos.SearchResponse response = service.search(owner, library.getId(), "leader", "PAGE", mode, 20);
            assertThat(response.mode()).isEqualTo(mode);
            assertThat(response.results()).extracting(SearchDtos.SearchResult::pageId).containsExactly(page.getId());
            assertThat(response.results().getFirst().origin().scope()).isEqualTo("CURRENT");
        }
        assertThatThrownBy(() -> service.search(UUID.randomUUID(), library.getId(), "leader", "PAGE", "KEYWORD", 20))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void referenced_search_requires_registered_direction() {
        UUID owner = UUID.randomUUID();
        UUID currentId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(libraryRepository.findByIdAndOwnerId(currentId, owner)).thenReturn(Optional.of(
                LibraryEntity.create(owner, "Current", null, TemplateType.CUSTOM)));
        assertThatThrownBy(() -> service.referencedResults(owner, currentId, targetId, "x",
                Set.of("PAGE"), "HYBRID", 10)).isInstanceOf(ApiException.class);
    }

    @Test
    void multiword_question_keeps_claim_when_one_token_matches_and_exposes_citation() {
        UUID owner = UUID.randomUUID();
        LibraryEntity library = LibraryEntity.create(owner, "Current", null, TemplateType.CUSTOM);
        PageEntity page = PageEntity.create(library.getId(), "phase1", "CONCEPT", "PUBLISHED");
        PageVersionEntity pageVersion = PageVersionEntity.imported(page.getId(), 1, "Phase 1", "phase1 knowledge", null, null);
        ClaimEntity claim = ClaimEntity.create(page.getId(), "phase1-claim", "phase1 is the indexed claim",
                BigDecimal.ONE);
        SourceEntity source = SourceEntity.create(owner, SourceType.FILE, "file://phase1.md", "Phase 1 source");
        SourceVersionEntity sourceVersion = SourceVersionEntity.create(source.getId(), 1, "a".repeat(64),
                "phase1.md", "text/markdown", 10, new ObjectMapper().createObjectNode(), CaptureMethod.UPLOAD);
        EvidenceEntity evidence = EvidenceEntity.create(claim.getId(), sourceVersion.getId(), "SUPPORTS",
                new ObjectMapper().createObjectNode().put("line", 1), "phase1 is the indexed claim", "en", BigDecimal.ONE);

        when(libraryRepository.findByIdAndOwnerId(library.getId(), owner)).thenReturn(Optional.of(library));
        when(pageRepository.findAllByLibraryIdOrderByUpdatedAtDesc(library.getId())).thenReturn(List.of(page));
        when(pageVersionRepository.findTopByPageIdOrderByVersionNoDesc(page.getId())).thenReturn(Optional.of(pageVersion));
        when(claimRepository.findAllByPageIdOrderByCreatedAtAsc(page.getId())).thenReturn(List.of(claim));
        when(evidenceRepository.findAllByClaimIdOrderByCreatedAtAsc(claim.getId())).thenReturn(List.of(evidence));
        when(sourceVersionRepository.findById(sourceVersion.getId())).thenReturn(Optional.of(sourceVersion));
        when(sourceRepository.findByIdAndOwnerId(source.getId(), owner)).thenReturn(Optional.of(source));

        SearchDtos.SearchResponse response = service.search(owner, library.getId(),
                "Explain phase1-unique-keyword-001 and cite evidence", "CLAIM", "KEYWORD", 20);

        assertThat(response.results()).hasSize(1);
        SearchDtos.SearchResult result = response.results().getFirst();
        assertThat(result.resultType()).isEqualTo("CLAIM");
        assertThat(result.claimId()).isEqualTo(claim.getId());
        assertThat(result.sourceVersionId()).isEqualTo(sourceVersion.getId());
        assertThat(result.locator()).isNotNull();
        assertThat(result.score()).isGreaterThan(0);
    }
}
