package com.zerowiki.library;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "libraries")
public class LibraryEntity {
    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 30)
    private TemplateType templateType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LibraryVisibility visibility;

    @Column(name = "current_constitution_version_id")
    private UUID currentConstitutionVersionId;

    @Column(name = "current_library_version_id")
    private UUID currentLibraryVersionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LibraryStatus status;

    @Version
    @Column(nullable = false)
    private long revision;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LibraryEntity() {
    }

    private LibraryEntity(UUID id, UUID ownerId, String name, String description, TemplateType templateType) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.templateType = templateType;
        this.visibility = LibraryVisibility.PRIVATE;
        this.status = LibraryStatus.ACTIVE;
        this.revision = 1;
    }

    public static LibraryEntity create(UUID ownerId, String name, String description, TemplateType templateType) {
        return new LibraryEntity(UUID.randomUUID(), ownerId, name.trim(), description, templateType);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public LibraryVisibility getVisibility() {
        return visibility;
    }

    public UUID getCurrentConstitutionVersionId() {
        return currentConstitutionVersionId;
    }

    public UUID getCurrentLibraryVersionId() {
        return currentLibraryVersionId;
    }

    public LibraryStatus getStatus() {
        return status;
    }

    public long getRevision() {
        return revision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void update(String name, String description) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (description != null) {
            this.description = description;
        }
    }

    public void archive() {
        status = LibraryStatus.ARCHIVED;
    }

    public void restore() {
        status = LibraryStatus.ACTIVE;
    }

    public void setCurrentConstitutionVersionId(UUID currentConstitutionVersionId) {
        this.currentConstitutionVersionId = currentConstitutionVersionId;
    }

    public void setCurrentLibraryVersionId(UUID currentLibraryVersionId) {
        this.currentLibraryVersionId = currentLibraryVersionId;
    }
}
