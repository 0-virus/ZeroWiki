package com.zerowiki.library;

import com.zerowiki.knowledge.ContradictionEntity;
import com.zerowiki.knowledge.ContradictionRepository;
import com.zerowiki.knowledge.PageEntity;
import com.zerowiki.knowledge.PageRelationEntity;
import com.zerowiki.knowledge.PageRelationRepository;
import com.zerowiki.knowledge.PageRepository;
import com.zerowiki.knowledge.PageVersionEntity;
import com.zerowiki.knowledge.PageVersionRepository;
import com.zerowiki.knowledge.ChangeSetEntity;
import com.zerowiki.knowledge.ChangeSetRepository;
import com.zerowiki.knowledge.LibraryVersionRepository;
import com.zerowiki.source.LibrarySourceRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LibraryHomeService {
    private final LibrarySourceRepository librarySourceRepository;
    private final PageRepository pageRepository;
    private final PageVersionRepository pageVersionRepository;
    private final PageRelationRepository relationRepository;
    private final ContradictionRepository contradictionRepository;
    private final ChangeSetRepository changeSetRepository;
    private final LibraryVersionRepository libraryVersionRepository;

    public LibraryHomeService(LibrarySourceRepository librarySourceRepository,
                              PageRepository pageRepository,
                              PageVersionRepository pageVersionRepository,
                              PageRelationRepository relationRepository,
                              ContradictionRepository contradictionRepository,
                              ChangeSetRepository changeSetRepository,
                              LibraryVersionRepository libraryVersionRepository) {
        this.librarySourceRepository = librarySourceRepository;
        this.pageRepository = pageRepository;
        this.pageVersionRepository = pageVersionRepository;
        this.relationRepository = relationRepository;
        this.contradictionRepository = contradictionRepository;
        this.changeSetRepository = changeSetRepository;
        this.libraryVersionRepository = libraryVersionRepository;
    }

    public Snapshot snapshot(LibraryEntity library) {
        UUID libraryId = library.getId();
        List<PageEntity> pages = pageRepository.findAllByLibraryIdOrderByUpdatedAtDesc(libraryId);
        List<PageRelationEntity> accepted = relationRepository.findAllByLibraryIdAndStatusOrderByCreatedAtDesc(
                libraryId, "ACCEPTED");
        Map<UUID, Long> relationCounts = accepted.stream()
                .flatMap(relation -> java.util.stream.Stream.of(relation.getSourcePageId(), relation.getTargetPageId()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<LibraryDtos.LibraryTopic> topics = pages.stream()
                .filter(page -> "CONCEPT".equals(page.getPageType()) && "PUBLISHED".equals(page.getStatus()))
                .map(page -> new TopicWithPage(page, relationCounts.getOrDefault(page.getId(), 0L).intValue()))
                .sorted(Comparator.comparingInt(TopicWithPage::pageCount).reversed()
                        .thenComparing(value -> value.page().getId()))
                .limit(6)
                .map(value -> new LibraryDtos.LibraryTopic(value.page().getId(), title(value.page()), value.pageCount()))
                .toList();
        long openContradictions = contradictionRepository
                .findAllByLibraryIdAndStatusOrderByCreatedAtDesc(libraryId, "OPEN").size();
        long pendingChanges = changeSetRepository.findAllByLibraryIdOrderByCreatedAtDesc(libraryId).stream()
                .filter(changeSet -> "READY_FOR_REVIEW".equals(changeSet.getStatus())
                        || "PARTIALLY_APPROVED".equals(changeSet.getStatus()))
                .count();
        long publishedPages = pages.stream().filter(page -> "PUBLISHED".equals(page.getStatus())).count();
        LibraryDtos.LibraryCounts counts = new LibraryDtos.LibraryCounts(
                librarySourceRepository.findAllByIdLibraryIdOrderByLinkedAtDesc(libraryId).size(),
                publishedPages, openContradictions, pendingChanges);
        LibraryDtos.LibraryHome home = new LibraryDtos.LibraryHome(topics, List.of(), List.of(), List.of(),
                openContradictions, pendingChanges, List.of());
        LibraryDtos.CurrentVersionResponse currentVersion = library.getCurrentLibraryVersionId() == null ? null
                : libraryVersionRepository.findById(library.getCurrentLibraryVersionId())
                .map(version -> new LibraryDtos.CurrentVersionResponse((int) version.getVersionNo()))
                .orElse(null);
        return new Snapshot(currentVersion, counts, home);
    }

    private String title(PageEntity page) {
        return pageVersionRepository.findTopByPageIdOrderByVersionNoDesc(page.getId())
                .map(PageVersionEntity::getTitle)
                .orElse(page.getSlug());
    }

    private record TopicWithPage(PageEntity page, int pageCount) {
    }

    public record Snapshot(LibraryDtos.CurrentVersionResponse currentVersion,
                           LibraryDtos.LibraryCounts counts, LibraryDtos.LibraryHome home) {
    }
}
