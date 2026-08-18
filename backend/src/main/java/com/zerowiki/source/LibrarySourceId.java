package com.zerowiki.source;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class LibrarySourceId implements Serializable {
    private UUID libraryId;
    private UUID sourceId;

    protected LibrarySourceId() {
    }

    public LibrarySourceId(UUID libraryId, UUID sourceId) {
        this.libraryId = libraryId;
        this.sourceId = sourceId;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LibrarySourceId that)) return false;
        return java.util.Objects.equals(libraryId, that.libraryId)
                && java.util.Objects.equals(sourceId, that.sourceId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(libraryId, sourceId);
    }
}
