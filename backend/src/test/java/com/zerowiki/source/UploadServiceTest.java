package com.zerowiki.source;

import com.zerowiki.common.ApiException;
import com.zerowiki.ingest.IngestService;
import com.zerowiki.library.LibraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {
    @Mock UploadSessionRepository uploadRepository;
    @Mock SourceService sourceService;
    @Mock IngestService ingestService;
    @Mock LibraryRepository libraryRepository;
    private UploadService service;
    private LocalObjectStore objectStore;

    @BeforeEach
    void setUp() throws Exception {
        objectStore = new LocalObjectStore(Files.createTempDirectory("zerowiki-upload"));
        service = new UploadService(uploadRepository, objectStore, sourceService, ingestService,
                libraryRepository, new ObjectMapper(), "http://localhost:8080");
    }

    @Test
    void plan_returns_absolute_tokenized_url_and_required_headers() {
        UUID owner = UUID.randomUUID();
        SourceDtos.UploadPlanResponse response = service.plan(owner,
                new SourceDtos.UploadPlanRequest("notes.md", "text/markdown", 5L, "a".repeat(64)));

        assertThat(response.uploadId()).isNotNull();
        assertThat(response.uploadUrl()).startsWith("http://localhost:8080/uploads/")
                .contains("/content?token=");
        assertThat(response.method()).isEqualTo("PUT");
        assertThat(response.requiredHeaders()).containsEntry("Content-Type", "text/markdown");
        assertThat(response.expiresAt()).isAfter(Instant.now());

        var saved = org.mockito.ArgumentCaptor.forClass(UploadSessionEntity.class);
        verify(uploadRepository).save(saved.capture());
        assertThat(saved.getValue().getUploadTokenHash()).hasSize(64);
        assertThat(saved.getValue().getUploadTokenHash()).doesNotContain(response.uploadUrl().substring(
                response.uploadUrl().indexOf("?token=") + 7));
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(response.expiresAt());
    }

    @Test
    void write_requires_matching_unexpired_token() {
        UUID uploadId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        UploadSessionEntity session = UploadSessionEntity.plan(owner, "notes.md", "text/markdown", content.length,
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                UploadService.tokenHash("valid-token"), Instant.now().plus(Duration.ofMinutes(15)), "uploads/test");
        when(uploadRepository.findByIdAndStatus(uploadId, UploadStatus.PLANNED)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.write(uploadId, "wrong-token", content))
                .isInstanceOf(ApiException.class)
                .hasMessage("upload not found");

        UploadSessionEntity expired = UploadSessionEntity.plan(owner, "notes.md", "text/markdown", content.length,
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                UploadService.tokenHash("expired-token"), Instant.now().minusSeconds(1), "uploads/expired");
        when(uploadRepository.findByIdAndStatus(uploadId, UploadStatus.PLANNED)).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.write(uploadId, "expired-token", content))
                .isInstanceOf(ApiException.class)
                .hasMessage("upload not found");
    }

    @Test
    void write_token_is_one_time_after_successful_upload() {
        UUID uploadId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        String rawToken = "one-time-token";
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        UploadSessionEntity session = UploadSessionEntity.plan(owner, "notes.md", "text/markdown", content.length,
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                UploadService.tokenHash(rawToken), Instant.now().plus(Duration.ofMinutes(15)), "uploads/once");
        when(uploadRepository.findByIdAndStatus(uploadId, UploadStatus.PLANNED)).thenReturn(Optional.of(session));

        service.write(uploadId, rawToken, content);

        assertThat(session.getStatus()).isEqualTo(UploadStatus.UPLOADED);
        assertThatThrownBy(() -> service.write(uploadId, rawToken, content))
                .isInstanceOf(ApiException.class)
                .hasMessage("upload is not accepting content");
    }

    @Test
    void complete_blocks_secret_before_creating_source() {
        UUID owner = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        byte[] content = "api_key=0123456789abcdef".getBytes();
        String rawToken = "secret-test-token";
        UploadSessionEntity session = UploadSessionEntity.plan(owner, "notes.md", "text/markdown", content.length,
                "268d7ffa123b91de97a2a18d176ce45722094bcc3926a08424b1e371a084c53a",
                UploadService.tokenHash(rawToken), Instant.now().plus(Duration.ofMinutes(15)), "uploads/test");
        when(uploadRepository.findByIdAndOwnerId(uploadId, owner)).thenReturn(Optional.of(session));
        when(uploadRepository.findByIdAndStatus(uploadId, UploadStatus.PLANNED)).thenReturn(Optional.of(session));
        service.write(uploadId, rawToken, content);

        assertThatThrownBy(() -> service.complete(owner, uploadId,
                new SourceDtos.UploadCompleteRequest(libraryId, SourceType.FILE, "notes", false, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("source contains a possible secret");
        assertThat(session.getStatus()).isEqualTo(UploadStatus.BLOCKED_SECRET);
    }
}
