package com.zerowiki.source;

import com.zerowiki.common.ApiException;
import com.zerowiki.library.LibraryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SourceService {
    private final SourceRepository sourceRepository;
    private final SourceVersionRepository versionRepository;
    private final LibrarySourceRepository librarySourceRepository;
    private final LibraryRepository libraryRepository;
    private final LocalObjectStore objectStore;

    public SourceService(SourceRepository sourceRepository,
                         SourceVersionRepository versionRepository,
                         LibrarySourceRepository librarySourceRepository,
                         LibraryRepository libraryRepository,
                         LocalObjectStore objectStore) {
        this.sourceRepository = sourceRepository;
        this.versionRepository = versionRepository;
        this.librarySourceRepository = librarySourceRepository;
        this.libraryRepository = libraryRepository;
        this.objectStore = objectStore;
    }

    @Transactional(readOnly = true)
    public List<SourceEntity> list(UUID ownerId, SourceType type, UUID libraryId, String query, int limit) {
        List<SourceEntity> candidates;
        if (libraryId != null) {
            requireLibrary(ownerId, libraryId);
            candidates = librarySourceRepository.findAllByIdLibraryIdOrderByLinkedAtDesc(libraryId).stream()
                    .map(link -> sourceRepository.findByIdAndOwnerId(link.getSourceId(), ownerId).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } else if (type != null) {
            candidates = sourceRepository.findAllByOwnerIdAndSourceTypeOrderByUpdatedAtDesc(ownerId, type);
        } else {
            candidates = sourceRepository.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId);
        }
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(source -> normalized.isBlank() || source.getTitle().toLowerCase(Locale.ROOT).contains(normalized)
                        || source.getCanonicalUri().toLowerCase(Locale.ROOT).contains(normalized))
                .limit(Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SourceEntity get(UUID ownerId, UUID sourceId) {
        return sourceRepository.findByIdAndOwnerId(sourceId, ownerId)
                .orElseThrow(() -> notFound("source not found"));
    }

    @Transactional(readOnly = true)
    public List<SourceVersionEntity> versions(UUID ownerId, UUID sourceId) {
        get(ownerId, sourceId);
        return versionRepository.findAllBySourceIdOrderByVersionNoDesc(sourceId);
    }

    @Transactional(readOnly = true)
    public SourceVersionEntity version(UUID ownerId, UUID sourceId, int versionNo) {
        get(ownerId, sourceId);
        return versionRepository.findBySourceIdAndVersionNo(sourceId, versionNo)
                .orElseThrow(() -> notFound("source version not found"));
    }

    @Transactional(readOnly = true)
    public SourceVersionEntity versionById(UUID ownerId, UUID versionId) {
        SourceVersionEntity version = versionRepository.findById(versionId)
                .orElseThrow(() -> notFound("source version not found"));
        get(ownerId, version.getSourceId());
        return version;
    }

    @Transactional
    public SourceVersionEntity create(UUID ownerId, UUID libraryId, SourceType sourceType, String canonicalUri,
                                      String title, String storageKey, String mediaType, long byteSize,
                                      String contentHash, JsonNode metadata, CaptureMethod captureMethod) {
        requireLibrary(ownerId, libraryId);
        SourceEntity source = sourceRepository.save(SourceEntity.create(ownerId, sourceType, canonicalUri, title));
        SourceVersionEntity version = versionRepository.save(SourceVersionEntity.create(source.getId(), 1,
                contentHash, storageKey, mediaType, byteSize, metadata, captureMethod));
        librarySourceRepository.save(LibrarySourceEntity.create(libraryId, source.getId()));
        return version;
    }

    @Transactional
    public SourceEntity rename(UUID ownerId, UUID sourceId, String title) {
        SourceEntity source = get(ownerId, sourceId);
        source.rename(title);
        return source;
    }

    @Transactional
    public void delete(UUID ownerId, UUID sourceId) {
        SourceEntity source = get(ownerId, sourceId);
        if (!librarySourceRepository.findAllByIdSourceIdOrderByLinkedAtDesc(sourceId).isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "SOURCE_REFERENCED", "source is still linked to a library");
        }
        source.markDeletionPending();
    }

    @Transactional(readOnly = true)
    public List<SourceVersionEntity> ownedVersions(UUID ownerId, List<UUID> versionIds) {
        List<SourceVersionEntity> result = new ArrayList<>();
        for (UUID versionId : versionIds) {
            result.add(versionById(ownerId, versionId));
        }
        return result;
    }

    public java.io.InputStream open(SourceVersionEntity version) {
        return objectStore.open(version.getStorageKey());
    }

    private void requireLibrary(UUID ownerId, UUID libraryId) {
        libraryRepository.findByIdAndOwnerId(libraryId, ownerId)
                .orElseThrow(() -> notFound("library not found"));
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
