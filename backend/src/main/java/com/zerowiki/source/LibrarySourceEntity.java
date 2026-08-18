package com.zerowiki.source;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "library_sources")
public class LibrarySourceEntity {
    @EmbeddedId
    private LibrarySourceId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Importance importance;

    @Column(name = "trust_override", precision = 4, scale = 3)
    private BigDecimal trustOverride;

    @Column(name = "context_note", columnDefinition = "text")
    private String contextNote;

    @CreationTimestamp
    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    protected LibrarySourceEntity() {
    }

    private LibrarySourceEntity(UUID libraryId, UUID sourceId) {
        this.id = new LibrarySourceId(libraryId, sourceId);
        this.importance = Importance.NORMAL;
    }

    public static LibrarySourceEntity create(UUID libraryId, UUID sourceId) {
        if (libraryId == null || sourceId == null) {
            throw new IllegalArgumentException("library and source are required");
        }
        return new LibrarySourceEntity(libraryId, sourceId);
    }

    public LibrarySourceId getId() {
        return id;
    }

    public UUID getLibraryId() {
        return id.getLibraryId();
    }

    public UUID getSourceId() {
        return id.getSourceId();
    }

    public Importance getImportance() {
        return importance;
    }

    public BigDecimal getTrustOverride() {
        return trustOverride;
    }

    public String getContextNote() {
        return contextNote;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public enum Importance {
        LOW,
        NORMAL,
        HIGH,
        CORE
    }
}
