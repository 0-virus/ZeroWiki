package com.zerowiki.library;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LibraryDtos {
    private LibraryDtos() {
    }

    public record OnboardingRequest(
            @NotBlank String goal,
            @NotNull @Size(min = 1) List<String> sourceTypes,
            @NotNull String knowledgeLevel,
            @NotBlank String trustedSources,
            @NotBlank String cautionAreas) {
    }

    public record CreateLibraryRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 2000) String description,
            @NotNull TemplateType templateType,
            @NotNull @Valid OnboardingRequest onboarding) {
    }

    public record UpdateLibraryRequest(@Size(max = 120) String name, @Size(max = 2000) String description) {
    }

    public record ConstitutionRequest(
            @NotNull Integer expectedVersion,
            @NotBlank String purpose,
            @NotBlank String audience,
            @NotBlank String knowledgeLevel,
            @NotNull JsonNode sourcePolicy,
            @NotNull JsonNode taxonomyPolicy,
            @NotNull JsonNode linkPolicy,
            @NotNull JsonNode stalenessPolicy,
            @NotNull JsonNode riskPolicy,
            @NotNull JsonNode privacyPolicy,
            @NotNull JsonNode rejectedPatterns,
            @NotBlank String naturalLanguageRules,
            @NotBlank String changeReason) {
        public JsonNode document(ObjectMapper mapper) {
            ObjectNode document = mapper.createObjectNode();
            document.put("purpose", purpose);
            document.put("audience", audience);
            document.put("knowledgeLevel", knowledgeLevel);
            document.set("sourcePolicy", sourcePolicy.deepCopy());
            document.set("taxonomyPolicy", taxonomyPolicy.deepCopy());
            document.set("linkPolicy", linkPolicy.deepCopy());
            document.set("stalenessPolicy", stalenessPolicy.deepCopy());
            document.set("riskPolicy", riskPolicy.deepCopy());
            document.set("privacyPolicy", privacyPolicy.deepCopy());
            document.set("rejectedPatterns", rejectedPatterns.deepCopy());
            document.put("naturalLanguageRules", naturalLanguageRules);
            document.put("changeReason", changeReason);
            return document;
        }
    }

    public record LibraryResponse(UUID id, String name, String description, TemplateType templateType,
                                  LibraryVisibility visibility, LibraryStatus status, Integer constitutionVersion,
                                  CurrentVersionResponse currentVersion, LibraryCounts counts, LibraryHome home,
                                  long revision, Instant createdAt) {
        static LibraryResponse from(LibraryEntity library, Integer constitutionVersion) {
            return from(library, constitutionVersion, null, LibraryCounts.empty(), LibraryHome.empty());
        }

        static LibraryResponse from(LibraryEntity library, Integer constitutionVersion,
                                    CurrentVersionResponse currentVersion, LibraryCounts counts, LibraryHome home) {
            return new LibraryResponse(library.getId(), library.getName(), library.getDescription(),
                    library.getTemplateType(), library.getVisibility(), library.getStatus(), constitutionVersion,
                    currentVersion, counts, home, library.getRevision(), library.getCreatedAt());
        }
    }

    public record CurrentVersionResponse(Integer versionNo) {
    }

    public record LibraryCounts(long sources, long pages, long openContradictions, long pendingChanges) {
        static LibraryCounts empty() {
            return new LibraryCounts(0, 0, 0, 0);
        }
    }

    public record LibraryHome(List<LibraryTopic> topics, List<Object> recentRelations, List<Object> openQuestions,
                              List<Object> knowledgeGaps, long openContradictionCount,
                              long pendingChangeSetCount, List<Object> recentActivities) {
        static LibraryHome empty() {
            return new LibraryHome(List.of(), List.of(), List.of(), List.of(), 0, 0, List.of());
        }
    }

    public record LibraryTopic(UUID pageId, String name, int pageCount) {
    }

    public record ConstitutionResponse(UUID id, UUID libraryId, int versionNo, JsonNode constitutionDocument,
                                       LibraryConstitutionEntity.CreatedByType createdByType, Instant createdAt,
                                       long revision) {
        static ConstitutionResponse from(LibraryConstitutionEntity constitution) {
            return new ConstitutionResponse(constitution.getId(), constitution.getLibraryId(), constitution.getVersionNo(),
                    constitution.getConstitutionDocument(), constitution.getCreatedByType(), constitution.getCreatedAt(),
                    constitution.getRevision());
        }
    }

    public record ReferenceRequest(@NotNull UUID targetLibraryId) {
    }

    public record ReferenceResponse(UUID id, UUID sourceLibraryId, UUID targetLibraryId, Instant createdAt) {
        static ReferenceResponse from(LibraryReferenceEntity reference) {
            return new ReferenceResponse(reference.getId(), reference.getSourceLibraryId(), reference.getTargetLibraryId(), reference.getCreatedAt());
        }
    }

    static JsonNode onboardingDocument(ObjectMapper mapper, CreateLibraryRequest request) {
        OnboardingRequest onboarding = request.onboarding();
        ObjectNode document = mapper.createObjectNode();
        document.put("purpose", onboarding.goal());
        document.put("audience", "도서관 사용자");
        document.put("knowledgeLevel", onboarding.knowledgeLevel());

        ObjectNode sourcePolicy = document.putObject("sourcePolicy");
        ArrayNode priority = sourcePolicy.putArray("priority");
        onboarding.sourceTypes().forEach(priority::add);
        sourcePolicy.put("minimumEvidenceForVerifiedFact", 1);

        ObjectNode taxonomyPolicy = document.putObject("taxonomyPolicy");
        ArrayNode preferredPageTypes = taxonomyPolicy.putArray("preferredPageTypes");
        preferredPageTypes.add("CONCEPT");
        preferredPageTypes.add("FLOW");
        preferredPageTypes.add("SYNTHESIS");
        preferredPageTypes.add("QUESTION");

        ObjectNode linkPolicy = document.putObject("linkPolicy");
        linkPolicy.put("minimumConfidence", 0.75);
        linkPolicy.put("avoidWeakSimilarity", true);

        document.putObject("stalenessPolicy").put("defaultReviewDays", 365);
        ObjectNode riskPolicy = document.putObject("riskPolicy");
        riskPolicy.put("factUpdate", "MAJOR");
        riskPolicy.put("deleteContent", "MAJOR");
        document.putObject("privacyPolicy").put("blockSecrets", true);
        document.putArray("rejectedPatterns");
        document.put("naturalLanguageRules", "trustedSources: " + onboarding.trustedSources()
                + "\ncautionAreas: " + onboarding.cautionAreas());
        document.put("changeReason", "온보딩으로 생성");
        return document;
    }
}
