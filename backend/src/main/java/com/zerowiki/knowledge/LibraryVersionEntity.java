package com.zerowiki.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "library_versions")
public class LibraryVersionEntity {
    @Id
    private UUID id;

    @Column(name = "library_id", nullable = false)
    private UUID libraryId;

    @Column(name = "version_no", nullable = false)
    private long versionNo;

    @Column(name = "change_set_id", nullable = false)
    private UUID changeSetId;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LibraryVersionEntity() {
    }

    private LibraryVersionEntity(UUID libraryId, long versionNo, UUID changeSetId,
                                 String summary, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.libraryId = libraryId;
        this.versionNo = versionNo;
        this.changeSetId = changeSetId;
        this.summary = summary;
        this.createdBy = createdBy;
    }

    static LibraryVersionEntity create(UUID libraryId, long versionNo, UUID changeSetId,
                                       String summary, UUID createdBy) {
        return new LibraryVersionEntity(libraryId, versionNo, changeSetId, summary, createdBy);
    }

    public UUID getId() {
        return id;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public long getVersionNo() {
        return versionNo;
    }

    public UUID getChangeSetId() {
        return changeSetId;
    }

    public String getSummary() {
        return summary;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
