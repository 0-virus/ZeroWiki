package com.zerowiki.library;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.zerowiki.common.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LibraryService {
    private final LibraryRepository libraryRepository;
    private final LibraryConstitutionRepository constitutionRepository;
    private final LibraryReferenceRepository referenceRepository;
    private final ConstitutionSchemaValidator schemaValidator;
    private final ObjectMapper objectMapper;

    public LibraryService(LibraryRepository libraryRepository,
                          LibraryConstitutionRepository constitutionRepository,
                          LibraryReferenceRepository referenceRepository,
                          ConstitutionSchemaValidator schemaValidator,
                          ObjectMapper objectMapper) {
        this.libraryRepository = libraryRepository;
        this.constitutionRepository = constitutionRepository;
        this.referenceRepository = referenceRepository;
        this.schemaValidator = schemaValidator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LibraryEntity create(UUID ownerId, LibraryDtos.CreateLibraryRequest request) {
        LibraryEntity library = libraryRepository.save(LibraryEntity.create(
                ownerId, request.name(), request.description(), request.templateType()));
        JsonNode document = LibraryDtos.onboardingDocument(objectMapper, request);
        schemaValidator.validate(document);
        LibraryConstitutionEntity constitution = constitutionRepository.save(
                LibraryConstitutionEntity.create(library.getId(), 1, document,
                        LibraryConstitutionEntity.CreatedByType.ONBOARDING));
        library.setCurrentConstitutionVersionId(constitution.getId());
        return libraryRepository.save(library);
    }

    @Transactional(readOnly = true)
    public List<LibraryEntity> list(UUID ownerId) {
        return libraryRepository.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public LibraryEntity get(UUID ownerId, UUID libraryId) {
        return libraryRepository.findByIdAndOwnerId(libraryId, ownerId)
                .orElseThrow(() -> notFound("도서관을 찾을 수 없습니다."));
    }

    @Transactional
    public LibraryEntity update(UUID ownerId, UUID libraryId, LibraryDtos.UpdateLibraryRequest request) {
        LibraryEntity library = get(ownerId, libraryId);
        library.update(request.name(), request.description());
        return library;
    }

    @Transactional
    public LibraryEntity archive(UUID ownerId, UUID libraryId) {
        LibraryEntity library = get(ownerId, libraryId);
        library.archive();
        return library;
    }

    @Transactional
    public LibraryEntity restore(UUID ownerId, UUID libraryId) {
        LibraryEntity library = get(ownerId, libraryId);
        library.restore();
        return library;
    }

    @Transactional(readOnly = true)
    public LibraryConstitutionEntity currentConstitution(UUID ownerId, UUID libraryId) {
        LibraryEntity library = get(ownerId, libraryId);
        if (library.getCurrentConstitutionVersionId() == null) {
            throw notFound("운영 헌법을 찾을 수 없습니다.");
        }
        return constitutionRepository.findByIdAndLibraryId(library.getCurrentConstitutionVersionId(), libraryId)
                .orElseThrow(() -> notFound("운영 헌법을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<LibraryConstitutionEntity> constitutionVersions(UUID ownerId, UUID libraryId) {
        get(ownerId, libraryId);
        return constitutionRepository.findAllByLibraryIdOrderByVersionNoDesc(libraryId);
    }

    @Transactional(readOnly = true)
    public LibraryConstitutionEntity constitutionVersion(UUID ownerId, UUID libraryId, int versionNo) {
        get(ownerId, libraryId);
        return constitutionRepository.findByLibraryIdAndVersionNo(libraryId, versionNo)
                .orElseThrow(() -> notFound("운영 헌법 버전을 찾을 수 없습니다."));
    }

    @Transactional
    public LibraryConstitutionEntity updateConstitution(UUID ownerId, UUID libraryId,
                                                         LibraryDtos.ConstitutionRequest request) {
        LibraryEntity library = get(ownerId, libraryId);
        LibraryConstitutionEntity current = currentConstitution(ownerId, libraryId);
        if (request.expectedVersion() != current.getVersionNo()) {
            throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "운영 헌법 버전이 변경되었습니다.");
        }
        JsonNode document = request.document(objectMapper);
        schemaValidator.validate(document);
        LibraryConstitutionEntity next = constitutionRepository.save(
                LibraryConstitutionEntity.create(libraryId, current.getVersionNo() + 1, document,
                        LibraryConstitutionEntity.CreatedByType.USER));
        library.setCurrentConstitutionVersionId(next.getId());
        libraryRepository.save(library);
        return next;
    }

    @Transactional(readOnly = true)
    public List<LibraryReferenceEntity> references(UUID ownerId, UUID libraryId) {
        get(ownerId, libraryId);
        return referenceRepository.findAllBySourceLibraryIdOrderByCreatedAtDesc(libraryId);
    }

    @Transactional
    public LibraryReferenceEntity addReference(UUID ownerId, UUID sourceLibraryId, UUID targetLibraryId) {
        if (sourceLibraryId.equals(targetLibraryId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_REFERENCE", "자기 자신을 참조할 수 없습니다.");
        }
        LibraryEntity source = get(ownerId, sourceLibraryId);
        LibraryEntity target = get(ownerId, targetLibraryId);
        try {
            return referenceRepository.save(LibraryReferenceEntity.create(source.getId(), target.getId(), ownerId));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "REFERENCE_ALREADY_EXISTS", "이미 등록된 참조입니다.");
        }
    }

    @Transactional
    public void removeReference(UUID ownerId, UUID sourceLibraryId, UUID targetLibraryId) {
        get(ownerId, sourceLibraryId);
        LibraryReferenceEntity reference = referenceRepository
                .findBySourceLibraryIdAndTargetLibraryId(sourceLibraryId, targetLibraryId)
                .orElseThrow(() -> notFound("참조를 찾을 수 없습니다."));
        referenceRepository.delete(reference);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
