package com.zerowiki.library;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "library_constitution_versions")
public class LibraryConstitutionEntity {
    @Id
    private UUID id;

    @Column(name = "library_id", nullable = false)
    private UUID libraryId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "constitution_document", nullable = false, columnDefinition = "jsonb")
    private JsonNode constitutionDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by_type", nullable = false, length = 20)
    private CreatedByType createdByType;

    @Version
    @Column(nullable = false)
    private long revision;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LibraryConstitutionEntity() {
    }

    private LibraryConstitutionEntity(UUID id, UUID libraryId, int versionNo, JsonNode constitutionDocument,
                                      CreatedByType createdByType) {
        this.id = id;
        this.libraryId = libraryId;
        this.versionNo = versionNo;
        this.constitutionDocument = constitutionDocument.deepCopy();
        this.createdByType = createdByType;
        this.revision = 1;
    }

    public static LibraryConstitutionEntity create(UUID libraryId, int versionNo, JsonNode document,
                                                    CreatedByType createdByType) {
        return new LibraryConstitutionEntity(UUID.randomUUID(), libraryId, versionNo, document, createdByType);
    }

    public UUID getId() {
        return id;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public JsonNode getConstitutionDocument() {
        return constitutionDocument.deepCopy();
    }

    public CreatedByType getCreatedByType() {
        return createdByType;
    }

    public long getRevision() {
        return revision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public enum CreatedByType {
        ONBOARDING,
        USER,
        FEEDBACK,
        SYSTEM
    }
}
