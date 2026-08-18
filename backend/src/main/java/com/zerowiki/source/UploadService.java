package com.zerowiki.source;

import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ObjectMapper;
import com.zerowiki.common.ApiException;
import com.zerowiki.ingest.ImportType;
import com.zerowiki.ingest.IngestDtos;
import com.zerowiki.ingest.IngestJobEntity;
import com.zerowiki.ingest.IngestService;
import com.zerowiki.ingest.ProcessingMode;
import com.zerowiki.library.LibraryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class UploadService {
    private static final long MAX_FILE_BYTES = 200L * 1024 * 1024;
    private static final long MAX_ZIP_BYTES = 1024L * 1024 * 1024;
    private static final Duration UPLOAD_TOKEN_TTL = Duration.ofMinutes(15);
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    private final UploadSessionRepository uploadRepository;
    private final LocalObjectStore objectStore;
    private final SourceService sourceService;
    private final IngestService ingestService;
    private final LibraryRepository libraryRepository;
    private final ObjectMapper objectMapper;
    private final String publicBaseUrl;

    public UploadService(UploadSessionRepository uploadRepository,
                         LocalObjectStore objectStore,
                         SourceService sourceService,
                         IngestService ingestService,
                         LibraryRepository libraryRepository,
                         ObjectMapper objectMapper,
                         @Value("${zerowiki.storage.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.uploadRepository = uploadRepository;
        this.objectStore = objectStore;
        this.sourceService = sourceService;
        this.ingestService = ingestService;
        this.libraryRepository = libraryRepository;
        this.objectMapper = objectMapper;
        this.publicBaseUrl = publicBaseUrl == null ? "http://localhost:8080"
                : publicBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public SourceDtos.UploadPlanResponse plan(UUID ownerId, SourceDtos.UploadPlanRequest request) {
        String sha256 = request.sha256().toLowerCase(Locale.ROOT);
        if (!sha256.matches("[0-9a-f]{64}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "sha256 must be hexadecimal");
        }
        if (request.fileName().contains("/") || request.fileName().contains("\\")
                || request.fileName().contains("..")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "file name is invalid");
        }
        long max = isZipName(request.fileName(), request.mediaType()) ? MAX_ZIP_BYTES : MAX_FILE_BYTES;
        if (request.byteSize() > max) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "file exceeds configured size limit");
        }
        String rawToken = newRawToken();
        Instant expiresAt = Instant.now().plus(UPLOAD_TOKEN_TTL);
        String mediaType = request.mediaType().trim().toLowerCase(Locale.ROOT);
        UploadSessionEntity session = UploadSessionEntity.plan(ownerId, request.fileName().trim(), mediaType,
                request.byteSize(), sha256, tokenHash(rawToken), expiresAt,
                "uploads/" + ownerId + "/" + UUID.randomUUID());
        uploadRepository.save(session);
        return new SourceDtos.UploadPlanResponse(session.getId(), publicBaseUrl + "/uploads/"
                + session.getId() + "/content?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8),
                "PUT", Map.of("Content-Type", mediaType), expiresAt);
    }

    @Transactional
    public void write(UUID uploadId, String rawToken, byte[] content) {
        UploadSessionEntity session = uploadRepository.findByIdAndStatus(uploadId, UploadStatus.PLANNED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "upload not found"));
        if (session.getStatus() != UploadStatus.PLANNED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_UPLOAD_STATE", "upload is not accepting content");
        }
        if (!validToken(session, rawToken)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "upload not found");
        }
        long max = isZipName(session.getFileName(), session.getMediaType()) ? MAX_ZIP_BYTES : MAX_FILE_BYTES;
        LocalObjectStore.StoredObject stored;
        try {
            stored = objectStore.write(session.getStorageKey(), new ByteArrayInputStream(content), max);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", exception.getMessage());
        }
        if (stored.byteSize() != session.getByteSize() || !stored.sha256().equals(session.getExpectedSha256())) {
            objectStore.delete(session.getStorageKey());
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "UPLOAD_HASH_MISMATCH",
                    "uploaded content does not match the upload plan");
        }
        session.markUploaded(stored.byteSize(), stored.sha256());
        uploadRepository.save(session);
    }

    static String tokenHash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String newRawToken() {
        byte[] token = new byte[32];
        TOKEN_RANDOM.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private boolean validToken(UploadSessionEntity session, String rawToken) {
        if (rawToken == null || rawToken.isBlank() || session.getExpiresAt() == null
                || !session.getExpiresAt().isAfter(Instant.now())) {
            return false;
        }
        byte[] expected = session.getUploadTokenHash().getBytes(StandardCharsets.UTF_8);
        byte[] actual = tokenHash(rawToken).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    @Transactional
    public SourceDtos.UploadCompleteResponse complete(UUID ownerId, UUID uploadId,
                                                       SourceDtos.UploadCompleteRequest request) {
        UploadSessionEntity session = session(ownerId, uploadId);
        if (session.getStatus() != UploadStatus.UPLOADED || !objectStore.exists(session.getStorageKey())) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_UPLOAD_STATE", "upload content is not complete");
        }
        byte[] content = objectStore.read(session.getStorageKey());
        List<SourceVersionEntity> versions = new ArrayList<>();
        if (isZip(content)) {
            List<ObsidianImporter.Entry> entries = ObsidianImporter.markdownEntries(new ByteArrayInputStream(content));
            if (entries.isEmpty()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_OBSIDIAN_IMPORT",
                        "Obsidian ZIP contains no Markdown files");
            }
            for (ObsidianImporter.Entry entry : entries) {
                if (SecretDetector.containsSecret(entry.content())) {
                    session.markBlockedSecret();
                    uploadRepository.save(session);
                    objectStore.delete(session.getStorageKey());
                    throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SECRET_DETECTED",
                            "source contains a possible secret");
                }
            }
            for (ObsidianImporter.Entry entry : entries) {
                String key = "sources/" + UUID.randomUUID();
                LocalObjectStore.StoredObject stored = objectStore.write(key,
                        new ByteArrayInputStream(entry.content()), MAX_FILE_BYTES);
                ObjectNode metadata = objectMapper.createObjectNode();
                metadata.put("archiveEntry", entry.name());
                versions.add(sourceService.create(ownerId, request.libraryId(), SourceType.OBSIDIAN,
                        "obsidian://" + uploadId + "/" + entry.name(),
                        request.title().trim() + " / " + entry.name(), key, "text/markdown",
                        stored.byteSize(), stored.sha256(), metadata, CaptureMethod.IMPORT));
            }
            objectStore.delete(session.getStorageKey());
        } else {
            if (SecretDetector.containsSecret(content)) {
                session.markBlockedSecret();
                uploadRepository.save(session);
                objectStore.delete(session.getStorageKey());
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SECRET_DETECTED",
                        "source contains a possible secret");
            }
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("fileName", session.getFileName());
            versions.add(sourceService.create(ownerId, request.libraryId(), request.sourceType(),
                    "upload://" + uploadId, request.title(), session.getStorageKey(), session.getMediaType(),
                    session.getByteSize(), session.getExpectedSha256(), metadata, CaptureMethod.UPLOAD));
        }
        session.markCompleted();
        uploadRepository.save(session);
        IngestDtos.IngestResponse ingest = null;
        if (request.shouldStartIngest()) {
            IngestJobEntity job = ingestService.create(ownerId, request.libraryId(),
                    new IngestDtos.CreateIngestRequest(
                            isZip(content) ? ImportType.OBSIDIAN_ZIP : ImportType.FILES,
                            request.effectiveProcessingMode(), versions.stream().map(SourceVersionEntity::getId).toList()));
            ingest = IngestDtos.IngestResponse.from(job);
        }
        List<SourceDtos.SourceResponse> sources = versions.stream()
                .map(version -> SourceDtos.SourceResponse.from(sourceService.get(ownerId, version.getSourceId()), version))
                .toList();
        return new SourceDtos.UploadCompleteResponse(sources, ingest);
    }

    @Transactional
    public void cancel(UUID ownerId, UUID uploadId) {
        UploadSessionEntity session = session(ownerId, uploadId);
        session.markCancelled();
        uploadRepository.save(session);
        objectStore.delete(session.getStorageKey());
    }

    private UploadSessionEntity session(UUID ownerId, UUID uploadId) {
        return uploadRepository.findByIdAndOwnerId(uploadId, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "upload not found"));
    }

    private boolean isZipName(String fileName, String mediaType) {
        return mediaType.equalsIgnoreCase("application/zip")
                || mediaType.equalsIgnoreCase("application/x-zip-compressed")
                || fileName.toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    private boolean isZip(byte[] bytes) {
        return bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K'
                && (bytes[2] == 3 || bytes[2] == 5 || bytes[2] == 7)
                && (bytes[3] == 4 || bytes[3] == 6 || bytes[3] == 8);
    }
}
