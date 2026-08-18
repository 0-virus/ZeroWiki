package com.zerowiki.knowledge;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pages")
public class PageEntity {
    @Id
    private UUID id;

    @Column(name = "library_id", nullable = false)
    private UUID libraryId;

    @Column(nullable = false, length = 240)
    private String slug;

    @Column(name = "page_type", nullable = false, length = 30)
    private String pageType;

    @Column(nullable = false, length = 20)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode topics;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PageEntity() {
    }

    public static PageEntity create(UUID libraryId, String slug, String pageType, String status) {
        return create(UUID.randomUUID(), libraryId, slug, pageType, status);
    }

    public static PageEntity createWithId(UUID id, UUID libraryId, String slug, String pageType, String status) {
        return create(id, libraryId, slug, pageType, status);
    }

    private static PageEntity create(UUID id, UUID libraryId, String slug, String pageType, String status) {
        if (id == null || libraryId == null || slug == null || slug.isBlank()
                || pageType == null || pageType.isBlank() || status == null || status.isBlank()) {
            throw new IllegalArgumentException("page metadata is invalid");
        }
        PageEntity page = new PageEntity();
        page.id = id;
        page.libraryId = libraryId;
        page.slug = slug;
        page.pageType = pageType;
        page.status = status;
        page.topics = new tools.jackson.databind.ObjectMapper().createArrayNode();
        return page;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public String getSlug() {
        return slug;
    }

    public String getPageType() {
        return pageType;
    }

    public String getStatus() {
        return status;
    }

    public JsonNode getTopics() {
        return topics == null ? null : topics.deepCopy();
    }

    public UUID getCurrentVersionId() {
        return currentVersionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void applyVersion(UUID versionId) {
        this.currentVersionId = versionId;
    }

    void applyStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("page status is required");
        }
        this.status = status;
    }
}
