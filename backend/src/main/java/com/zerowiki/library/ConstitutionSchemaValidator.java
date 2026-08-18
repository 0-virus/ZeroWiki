package com.zerowiki.library;

import com.zerowiki.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Set;

@Component
public class ConstitutionSchemaValidator {
    private static final Set<String> ROOT_FIELDS = Set.of(
            "purpose", "audience", "knowledgeLevel", "sourcePolicy", "taxonomyPolicy", "linkPolicy",
            "stalenessPolicy", "riskPolicy", "privacyPolicy", "rejectedPatterns", "naturalLanguageRules", "changeReason");
    private static final Set<String> KNOWLEDGE_LEVELS = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED");
    private static final Set<String> PAGE_TYPES = Set.of("CONCEPT", "FLOW", "SYNTHESIS", "QUESTION", "SOURCE");
    private static final Set<String> RISK_LEVELS = Set.of("MINOR", "MAJOR");

    public void validate(JsonNode document) {
        requireObject(document, "constitution document");
        requireExactFields(document, ROOT_FIELDS, "constitution document");
        requireText(document, "purpose");
        requireText(document, "audience");
        requireEnum(document, "knowledgeLevel", KNOWLEDGE_LEVELS);

        JsonNode sourcePolicy = requiredObject(document, "sourcePolicy");
        requireExactFields(sourcePolicy, Set.of("priority", "minimumEvidenceForVerifiedFact"), "sourcePolicy");
        requireStringArray(sourcePolicy, "priority", 1, true);
        requireInteger(sourcePolicy, "minimumEvidenceForVerifiedFact", 1);

        JsonNode taxonomyPolicy = requiredObject(document, "taxonomyPolicy");
        requireExactFields(taxonomyPolicy, Set.of("preferredPageTypes"), "taxonomyPolicy");
        requireStringArray(taxonomyPolicy, "preferredPageTypes", 1, true);
        for (JsonNode pageType : taxonomyPolicy.path("preferredPageTypes")) {
            if (!PAGE_TYPES.contains(pageType.asText())) {
                throw invalid("taxonomyPolicy.preferredPageTypes contains an unsupported page type");
            }
        }

        JsonNode linkPolicy = requiredObject(document, "linkPolicy");
        requireExactFields(linkPolicy, Set.of("minimumConfidence", "avoidWeakSimilarity"), "linkPolicy");
        requireNumber(linkPolicy, "minimumConfidence", 0, 1);
        requireBoolean(linkPolicy, "avoidWeakSimilarity");

        JsonNode stalenessPolicy = requiredObject(document, "stalenessPolicy");
        requireExactFields(stalenessPolicy, Set.of("defaultReviewDays"), "stalenessPolicy");
        requireInteger(stalenessPolicy, "defaultReviewDays", 1);

        JsonNode riskPolicy = requiredObject(document, "riskPolicy");
        requireExactFields(riskPolicy, Set.of("factUpdate", "deleteContent"), "riskPolicy");
        requireEnum(riskPolicy, "factUpdate", RISK_LEVELS);
        requireEnum(riskPolicy, "deleteContent", RISK_LEVELS);

        JsonNode privacyPolicy = requiredObject(document, "privacyPolicy");
        requireExactFields(privacyPolicy, Set.of("blockSecrets"), "privacyPolicy");
        requireBoolean(privacyPolicy, "blockSecrets");

        JsonNode rejectedPatterns = document.path("rejectedPatterns");
        if (!rejectedPatterns.isArray()) {
            throw invalid("rejectedPatterns must be an array");
        }
        for (JsonNode pattern : rejectedPatterns) {
            requireObject(pattern, "rejectedPatterns item");
            requireExactFields(pattern, Set.of("pattern", "reason"), "rejectedPatterns item");
            requireText(pattern, "pattern");
            requireText(pattern, "reason");
        }

        requireText(document, "naturalLanguageRules");
        requireText(document, "changeReason");
    }

    private JsonNode requiredObject(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        requireObject(value, field);
        return value;
    }

    private void requireObject(JsonNode value, String field) {
        if (value == null || !value.isObject()) {
            throw invalid(field + " must be an object");
        }
    }

    private void requireExactFields(JsonNode object, Set<String> allowed, String field) {
        for (String actual : object.propertyNames()) {
            if (!allowed.contains(actual)) {
                throw invalid(field + " contains an unsupported field: " + actual);
            }
        }
        for (String required : requiredFields(field)) {
            if (!object.has(required)) {
                throw invalid(field + " is missing required field: " + required);
            }
        }
    }

    private Set<String> requiredFields(String field) {
        return switch (field) {
            case "constitution document" -> ROOT_FIELDS;
            case "sourcePolicy" -> Set.of("priority", "minimumEvidenceForVerifiedFact");
            case "taxonomyPolicy" -> Set.of("preferredPageTypes");
            case "linkPolicy" -> Set.of("minimumConfidence", "avoidWeakSimilarity");
            case "stalenessPolicy" -> Set.of("defaultReviewDays");
            case "riskPolicy" -> Set.of("factUpdate", "deleteContent");
            case "privacyPolicy" -> Set.of("blockSecrets");
            case "rejectedPatterns item" -> Set.of("pattern", "reason");
            default -> Set.of();
        };
    }

    private void requireText(JsonNode object, String field) {
        JsonNode value = object.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw invalid(field + " must be a non-empty string");
        }
    }

    private void requireEnum(JsonNode object, String field, Set<String> allowed) {
        JsonNode value = object.path(field);
        if (!value.isTextual() || !allowed.contains(value.asText())) {
            throw invalid(field + " contains an unsupported value");
        }
    }

    private void requireStringArray(JsonNode object, String field, int minimumSize, boolean unique) {
        JsonNode value = object.path(field);
        if (!value.isArray() || value.size() < minimumSize) {
            throw invalid(field + " must be an array with at least " + minimumSize + " item(s)");
        }
        Set<String> values = new HashSet<>();
        for (JsonNode item : value) {
            if (!item.isTextual() || item.asText().isBlank() || (unique && !values.add(item.asText()))) {
                throw invalid(field + " must contain unique non-empty strings");
            }
        }
    }

    private void requireInteger(JsonNode object, String field, long minimum) {
        JsonNode value = object.path(field);
        if (!value.isIntegralNumber() || value.asLong() < minimum) {
            throw invalid(field + " must be an integer >= " + minimum);
        }
    }

    private void requireNumber(JsonNode object, String field, double minimum, double maximum) {
        JsonNode value = object.path(field);
        double number = value.asDouble(Double.NaN);
        if (!value.isNumber() || !Double.isFinite(number) || number < minimum || number > maximum) {
            throw invalid(field + " must be a number between " + minimum + " and " + maximum);
        }
    }

    private void requireBoolean(JsonNode object, String field) {
        if (!object.path(field).isBoolean()) {
            throw invalid(field + " must be a boolean");
        }
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONSTITUTION_SCHEMA_INVALID", message);
    }
}
