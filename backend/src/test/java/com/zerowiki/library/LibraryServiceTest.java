package com.zerowiki.library;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import com.zerowiki.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {
    @Mock
    LibraryRepository libraryRepository;
    @Mock
    LibraryConstitutionRepository constitutionRepository;
    @Mock
    LibraryReferenceRepository referenceRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConstitutionSchemaValidator schemaValidator = new ConstitutionSchemaValidator();
    private LibraryService service;

    @BeforeEach
    void setUp() {
        service = new LibraryService(libraryRepository, constitutionRepository, referenceRepository,
                schemaValidator, objectMapper);
    }

    @Test
    void create_defaults_library_to_private() {
        UUID ownerId = UUID.randomUUID();
        when(libraryRepository.save(any(LibraryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(constitutionRepository.save(any(LibraryConstitutionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LibraryEntity library = service.create(ownerId, new LibraryDtos.CreateLibraryRequest(
                "개인 도서관", "설명", TemplateType.LEARNING_RESEARCH,
                new LibraryDtos.OnboardingRequest("학습", java.util.List.of("PAPER"), "BEGINNER", "공식 문서", "주의")));

        assertThat(library.getVisibility()).isEqualTo(LibraryVisibility.PRIVATE);
        assertThat(library.getOwnerId()).isEqualTo(ownerId);
        assertThat(library.getCurrentConstitutionVersionId()).isNotNull();
        var constitutionCaptor = org.mockito.ArgumentCaptor.forClass(LibraryConstitutionEntity.class);
        verify(constitutionRepository).save(constitutionCaptor.capture());
        JsonNode sourcePolicy = constitutionCaptor.getValue().getConstitutionDocument().path("sourcePolicy");
        assertThat(sourcePolicy.path("priority").toString()).isEqualTo("[\"PAPER\"]");
        assertThat(constitutionCaptor.getValue().getConstitutionDocument().path("naturalLanguageRules").asText())
                .contains("trustedSources:")
                .contains("cautionAreas:");
    }

    @Test
    void get_rejects_library_owned_by_another_user() {
        UUID ownerId = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        when(libraryRepository.findByIdAndOwnerId(libraryId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(ownerId, libraryId))
                .isInstanceOf(ApiException.class)
                .hasMessage("도서관을 찾을 수 없습니다.");
    }

    @Test
    void constitution_update_creates_new_immutable_version() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID libraryId = UUID.randomUUID();
        LibraryEntity library = LibraryEntity.create(ownerId, "도서관", null, TemplateType.CUSTOM);
        LibraryConstitutionEntity current = LibraryConstitutionEntity.create(libraryId, 1,
                validDocument("기존 목적"), LibraryConstitutionEntity.CreatedByType.ONBOARDING);
        library.setCurrentConstitutionVersionId(current.getId());
        when(libraryRepository.findByIdAndOwnerId(libraryId, ownerId)).thenReturn(Optional.of(library));
        when(constitutionRepository.findByIdAndLibraryId(current.getId(), libraryId)).thenReturn(Optional.of(current));
        when(constitutionRepository.save(any(LibraryConstitutionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LibraryConstitutionEntity next = service.updateConstitution(ownerId, libraryId,
                request(1, "새 목적"));

        assertThat(next.getVersionNo()).isEqualTo(2);
        assertThat(current.getConstitutionDocument().path("purpose").asText()).isEqualTo("기존 목적");
        verify(constitutionRepository).save(any(LibraryConstitutionEntity.class));
    }

    @Test
    void reference_is_directional_and_self_reference_is_rejected() {
        UUID ownerId = UUID.randomUUID();
        LibraryEntity source = LibraryEntity.create(ownerId, "출발", null, TemplateType.CUSTOM);
        LibraryEntity target = LibraryEntity.create(ownerId, "도착", null, TemplateType.CUSTOM);
        UUID sourceId = source.getId();
        UUID targetId = target.getId();
        when(libraryRepository.findByIdAndOwnerId(sourceId, ownerId)).thenReturn(Optional.of(source));
        when(libraryRepository.findByIdAndOwnerId(targetId, ownerId)).thenReturn(Optional.of(target));
        when(referenceRepository.save(any(LibraryReferenceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LibraryReferenceEntity reference = service.addReference(ownerId, sourceId, targetId);

        assertThat(reference.getSourceLibraryId()).isEqualTo(sourceId);
        assertThat(reference.getTargetLibraryId()).isEqualTo(targetId);
        assertThatThrownBy(() -> service.addReference(ownerId, sourceId, sourceId))
                .isInstanceOf(ApiException.class)
                .hasMessage("자기 자신을 참조할 수 없습니다.");
    }

    @Test
    void constitution_validator_enforces_nested_enum_minimum_and_unknown_fields() throws Exception {
        ObjectNode invalidEnum = (ObjectNode) validDocument("목적");
        invalidEnum.put("knowledgeLevel", "EXPERT");
        assertThatThrownBy(() -> schemaValidator.validate(invalidEnum)).isInstanceOf(ApiException.class);

        ObjectNode invalidMinimum = (ObjectNode) validDocument("목적");
        ((ObjectNode) invalidMinimum.path("sourcePolicy")).putArray("priority");
        assertThatThrownBy(() -> schemaValidator.validate(invalidMinimum)).isInstanceOf(ApiException.class);

        ObjectNode unknownField = (ObjectNode) validDocument("목적");
        ((ObjectNode) unknownField.path("privacyPolicy")).put("unexpected", true);
        assertThatThrownBy(() -> schemaValidator.validate(unknownField)).isInstanceOf(ApiException.class);
    }

    private JsonNode validDocument(String purpose) throws Exception {
        return objectMapper.readTree("""
                {
                  "purpose":"%s",
                  "audience":"사용자",
                  "knowledgeLevel":"BEGINNER",
                  "sourcePolicy":{"priority":["OFFICIAL_DOCS"],"minimumEvidenceForVerifiedFact":1},
                  "taxonomyPolicy":{"preferredPageTypes":["CONCEPT"]},
                  "linkPolicy":{"minimumConfidence":0.75,"avoidWeakSimilarity":true},
                  "stalenessPolicy":{"defaultReviewDays":365},
                  "riskPolicy":{"factUpdate":"MAJOR","deleteContent":"MAJOR"},
                  "privacyPolicy":{"blockSecrets":true},
                  "rejectedPatterns":[],
                  "naturalLanguageRules":"규칙",
                  "changeReason":"변경"
                }
                """.formatted(purpose));
    }

    private LibraryDtos.ConstitutionRequest request(int expectedVersion, String purpose) throws Exception {
        JsonNode document = validDocument(purpose);
        return new LibraryDtos.ConstitutionRequest(expectedVersion, purpose, document.path("audience").asText(),
                document.path("knowledgeLevel").asText(), document.path("sourcePolicy"), document.path("taxonomyPolicy"),
                document.path("linkPolicy"), document.path("stalenessPolicy"), document.path("riskPolicy"),
                document.path("privacyPolicy"), document.path("rejectedPatterns"),
                document.path("naturalLanguageRules").asText(), document.path("changeReason").asText());
    }
}
