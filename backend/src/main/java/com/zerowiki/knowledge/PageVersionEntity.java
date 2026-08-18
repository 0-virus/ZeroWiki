package com.zerowiki.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Entity
@Table(name = "page_versions")
public class PageVersionEntity {
    @Id
    private UUID id;

    @Column(name = "page_id", nullable = false)
    private UUID pageId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "markdown_body", nullable = false, columnDefinition = "text")
    private String markdownBody;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "change_set_id")
    private UUID changeSetId;

    @Column(name = "created_by_type", nullable = false, length = 20)
    private String createdByType;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PageVersionEntity() {
    }

    private PageVersionEntity(UUID pageId, int versionNo, String title, String markdownBody, String summary,
                              String contentHash, UUID changeSetId, String createdByType, UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.pageId = pageId;
        this.versionNo = versionNo;
        this.title = title;
        this.markdownBody = markdownBody;
        this.summary = summary;
        this.contentHash = contentHash;
        this.changeSetId = changeSetId;
        this.createdByType = createdByType;
        this.createdByUserId = createdByUserId;
    }

    public static PageVersionEntity create(UUID pageId, int versionNo, String title, String markdownBody, String summary,
                                           UUID changeSetId, UUID createdByUserId) {
        if (pageId == null || versionNo < 1 || title == null || title.isBlank()
                || markdownBody == null || createdByUserId == null) {
            throw new IllegalArgumentException("page version metadata is invalid");
        }
        return new PageVersionEntity(pageId, versionNo, title, markdownBody, summary,
                sha256(markdownBody), changeSetId, "USER", createdByUserId);
    }

    public static PageVersionEntity imported(UUID pageId, int versionNo, String title, String markdownBody,
                                             String summary, UUID changeSetId) {
        if (pageId == null || versionNo < 1 || title == null || title.isBlank()
                || markdownBody == null) {
            throw new IllegalArgumentException("import page version metadata is invalid");
        }
        return new PageVersionEntity(pageId, versionNo, title, markdownBody, summary,
                sha256(markdownBody), changeSetId, "IMPORT", null);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPageId() {
        return pageId;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public String getTitle() {
        return title;
    }

    public String getMarkdownBody() {
        return markdownBody;
    }

    public String getSummary() {
        return summary;
    }

    public String getContentHash() {
        return contentHash;
    }

    public UUID getChangeSetId() {
        return changeSetId;
    }

    public String getCreatedByType() {
        return createdByType;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
