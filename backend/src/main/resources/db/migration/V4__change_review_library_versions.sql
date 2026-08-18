ALTER TABLE change_items
    ADD COLUMN risk_level varchar(20) NOT NULL DEFAULT 'MINOR',
    ADD COLUMN reason text,
    ADD COLUMN evidence_summary jsonb,
    ADD COLUMN ai_confidence numeric(4,3),
    ADD COLUMN review_status varchar(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE change_items
    ADD CONSTRAINT change_items_risk_level_check CHECK (risk_level IN ('MINOR', 'MAJOR')),
    ADD CONSTRAINT change_items_review_status_check
        CHECK (review_status IN ('PENDING', 'APPROVED', 'REJECTED', 'REVISION_REQUESTED', 'DEFERRED'));

CREATE INDEX change_items_review_status_idx ON change_items (change_set_id, review_status);

CREATE TABLE change_reviews (
    id uuid PRIMARY KEY,
    change_set_id uuid NOT NULL REFERENCES change_sets(id) ON DELETE CASCADE,
    change_item_id uuid REFERENCES change_items(id) ON DELETE CASCADE,
    reviewer_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    decision varchar(30) NOT NULL,
    comment text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT change_reviews_decision_check
        CHECK (decision IN ('APPROVE', 'REJECT', 'REQUEST_CHANGES', 'DEFER')),
    CONSTRAINT change_reviews_comment_check
        CHECK (decision NOT IN ('REJECT', 'REQUEST_CHANGES') OR nullif(trim(comment), '') IS NOT NULL)
);

CREATE INDEX change_reviews_change_set_idx ON change_reviews (change_set_id, created_at DESC);
CREATE INDEX change_reviews_change_item_idx ON change_reviews (change_item_id, created_at DESC);

CREATE TABLE library_versions (
    id uuid PRIMARY KEY,
    library_id uuid NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    version_no bigint NOT NULL,
    change_set_id uuid NOT NULL UNIQUE REFERENCES change_sets(id) ON DELETE RESTRICT,
    summary text,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (library_id, version_no)
);

ALTER TABLE libraries
    ADD CONSTRAINT libraries_current_library_version_fk
    FOREIGN KEY (current_library_version_id) REFERENCES library_versions(id) ON DELETE RESTRICT;

CREATE INDEX library_versions_library_created_idx
    ON library_versions (library_id, version_no DESC);

CREATE FUNCTION reject_library_version_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'library versions are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER library_versions_immutable
    BEFORE UPDATE OR DELETE ON library_versions
    FOR EACH ROW EXECUTE FUNCTION reject_library_version_mutation();
