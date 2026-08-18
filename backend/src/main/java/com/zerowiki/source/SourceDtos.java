package com.zerowiki.source;

import com.zerowiki.ingest.IngestDtos;
import com.zerowiki.ingest.ProcessingMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SourceDtos {
    private SourceDtos() {
    }

    public record UploadPlanRequest(
            @NotBlank @Size(max = 255) String fileName,
            @NotBlank @Size(max = 120) String mediaType,
            @NotNull @Positive Long byteSize,
            @NotBlank @Size(min = 64, max = 64) String sha256) {
    }

    public record UploadPlanResponse(UUID uploadId, String uploadUrl, String method,
                                     Map<String, String> requiredHeaders, Instant expiresAt) {
    }

    public record UploadCompleteRequest(
            @NotNull UUID libraryId,
            @NotNull SourceType sourceType,
            @NotBlank @Size(max = 500) String title,
            Boolean startIngest,
            ProcessingMode processingMode) {
        public boolean shouldStartIngest() {
            return Boolean.TRUE.equals(startIngest);
        }

        public ProcessingMode effectiveProcessingMode() {
            return processingMode == null ? ProcessingMode.STANDARD : processingMode;
        }
    }

    public record UploadCompleteResponse(List<SourceResponse> sources, IngestDtos.IngestResponse ingestJob) {
    }

    public record SourceResponse(UUID id, SourceType sourceType, String canonicalUri, String title,
                                 SourceStatus status, SourceVersionResponse latestVersion,
                                 Instant createdAt, Instant updatedAt) {
        static SourceResponse from(SourceEntity source, SourceVersionEntity latest) {
            return new SourceResponse(source.getId(), source.getSourceType(), source.getCanonicalUri(),
                    source.getTitle(), source.getStatus(), latest == null ? null : SourceVersionResponse.from(latest),
                    source.getCreatedAt(), source.getUpdatedAt());
        }
    }

    public record SourceVersionResponse(UUID id, UUID sourceId, int versionNo, String contentHash,
                                        String mediaType, long byteSize, String detectedLanguage,
                                        Object sourceMetadata, CaptureMethod captureMethod,
                                        Instant capturedAt, Instant createdAt) {
        static SourceVersionResponse from(SourceVersionEntity version) {
            return new SourceVersionResponse(version.getId(), version.getSourceId(), version.getVersionNo(),
                    version.getContentHash(), version.getMediaType(), version.getByteSize(),
                    version.getDetectedLanguage(), version.getSourceMetadata(), version.getCaptureMethod(),
                    version.getCapturedAt(), version.getCreatedAt());
        }
    }

    public record UpdateSourceRequest(@NotBlank @Size(max = 500) String title) {
    }
}
