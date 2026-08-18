package com.zerowiki.source;

import com.zerowiki.common.ApiException;
import com.zerowiki.library.LibraryEntity;
import com.zerowiki.library.LibraryRepository;
import com.zerowiki.library.TemplateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SourceServiceTest {
    @Mock SourceRepository sourceRepository;
    @Mock SourceVersionRepository versionRepository;
    @Mock LibrarySourceRepository librarySourceRepository;
    @Mock LibraryRepository libraryRepository;
    private SourceService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new SourceService(sourceRepository, versionRepository, librarySourceRepository,
                libraryRepository, new LocalObjectStore(Files.createTempDirectory("zerowiki-source")));
    }

    @Test
    void get_does_not_reveal_source_owned_by_another_user() {
        UUID owner = UUID.randomUUID();
        when(sourceRepository.findByIdAndOwnerId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(owner, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessage("source not found");
    }

    @Test
    void delete_rejects_a_source_still_linked_to_a_library() {
        UUID owner = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        SourceEntity source = SourceEntity.create(owner, SourceType.FILE, "upload://x", "x");
        when(sourceRepository.findByIdAndOwnerId(sourceId, owner)).thenReturn(Optional.of(source));
        when(librarySourceRepository.findAllByIdSourceIdOrderByLinkedAtDesc(sourceId))
                .thenReturn(List.of(LibrarySourceEntity.create(UUID.randomUUID(), sourceId)));

        assertThatThrownBy(() -> service.delete(owner, sourceId))
                .isInstanceOf(ApiException.class)
                .hasMessage("source is still linked to a library");
    }
}
