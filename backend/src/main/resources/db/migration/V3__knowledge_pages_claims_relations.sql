CREATE TABLE change_sets (
    id uuid PRIMARY KEY,
    library_id uuid NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    origin_type varchar(30) NOT NULL,
    origin_id uuid,
    title text NOT NULL,
    summary text,
    risk_level varchar(20) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'DRAFT',
    revision bigint NOT NULL DEFAULT 1,
    base_library_version_id uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    reviewed_at timestamptz,
    applied_at timestamptz,
    CONSTRAINT change_sets_origin_type_check CHECK (origin_type IN ('INGEST', 'ASSISTANT', 'LINT', 'USER_EDIT', 'FORK_UPDATE', 'EDIT_PROPOSAL')),
    CONSTRAINT change_sets_risk_level_check CHECK (risk_level IN ('MINOR', 'MAJOR')),
    CONSTRAINT change_sets_status_check CHECK (status IN ('DRAFT', 'READY_FOR_REVIEW', 'PARTIALLY_APPROVED', 'APPROVED', 'REJECTED', 'APPLIED', 'SUPERSEDED'))
);

CREATE INDEX change_sets_library_created_idx ON change_sets (library_id, created_at DESC);

CREATE TABLE change_items (
    id uuid PRIMARY KEY,
    change_set_id uuid NOT NULL REFERENCES change_sets(id) ON DELETE CASCADE,
    entity_type varchar(40) NOT NULL,
    entity_id uuid NOT NULL,
    operation varchar(20) NOT NULL,
    before_snapshot jsonb,
    after_snapshot jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT change_items_operation_check CHECK (operation IN ('CREATE', 'UPDATE', 'DELETE'))
);

CREATE INDEX change_items_change_set_idx ON change_items (change_set_id, created_at);

CREATE TABLE pages (
    id uuid PRIMARY KEY,
    library_id uuid NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    slug varchar(240) NOT NULL,
    page_type varchar(30) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'DRAFT',
    topics jsonb NOT NULL DEFAULT '[]'::jsonb,
    current_version_id uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pages_type_check CHECK (page_type IN ('SOURCE', 'CONCEPT', 'FLOW', 'SYNTHESIS', 'DECISION', 'QUESTION')),
    CONSTRAINT pages_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'DELETED')),
    UNIQUE (library_id, slug)
);

CREATE INDEX pages_library_status_updated_idx ON pages (library_id, status, updated_at DESC);
CREATE INDEX pages_library_type_idx ON pages (library_id, page_type);

CREATE TABLE page_versions (
    id uuid PRIMARY KEY,
    page_id uuid NOT NULL REFERENCES pages(id) ON DELETE RESTRICT,
    version_no integer NOT NULL,
    title text NOT NULL,
    markdown_body text NOT NULL,
    summary text,
    content_hash varchar(64) NOT NULL,
    change_set_id uuid REFERENCES change_sets(id) ON DELETE RESTRICT,
    created_by_type varchar(20) NOT NULL,
    created_by_user_id uuid REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT page_versions_created_by_type_check CHECK (created_by_type IN ('AI', 'USER', 'IMPORT', 'SYSTEM')),
    CONSTRAINT page_versions_version_check CHECK (version_no > 0),
    UNIQUE (page_id, version_no)
);

ALTER TABLE pages
    ADD CONSTRAINT pages_current_version_fk
    FOREIGN KEY (current_version_id) REFERENCES page_versions(id) ON DELETE RESTRICT;

CREATE INDEX page_versions_page_created_idx ON page_versions (page_id, version_no DESC);

CREATE TABLE claims (
    id uuid PRIMARY KEY,
    page_id uuid NOT NULL REFERENCES pages(id) ON DELETE RESTRICT,
    claim_key varchar(120) NOT NULL,
    statement text NOT NULL,
    ai_confidence numeric(4,3),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (page_id, claim_key)
);

CREATE INDEX claims_page_idx ON claims (page_id, created_at);

CREATE TABLE claim_statuses (
    id uuid PRIMARY KEY,
    claim_id uuid NOT NULL REFERENCES claims(id) ON DELETE RESTRICT,
    page_version_id uuid NOT NULL REFERENCES page_versions(id) ON DELETE RESTRICT,
    knowledge_status varchar(30) NOT NULL,
    confidence numeric(4,3),
    valid_from date,
    valid_to date,
    conditions jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT claim_statuses_knowledge_status_check CHECK (knowledge_status IN ('VERIFIED_FACT', 'SOURCE_CLAIM', 'AI_SYNTHESIS', 'USER_JUDGMENT', 'HYPOTHESIS')),
    UNIQUE (claim_id, page_version_id)
);

CREATE INDEX claim_statuses_claim_version_idx ON claim_statuses (claim_id, page_version_id);

CREATE TABLE evidences (
    id uuid PRIMARY KEY,
    claim_id uuid NOT NULL REFERENCES claims(id) ON DELETE RESTRICT,
    source_version_id uuid NOT NULL REFERENCES source_versions(id) ON DELETE RESTRICT,
    evidence_type varchar(20) NOT NULL,
    locator jsonb NOT NULL,
    excerpt text NOT NULL,
    excerpt_language varchar(20) NOT NULL,
    source_trust numeric(4,3),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT evidences_type_check CHECK (evidence_type IN ('SUPPORTS', 'REFUTES', 'CONTEXT')),
    CONSTRAINT evidences_excerpt_length_check CHECK (char_length(excerpt) <= 400)
);

CREATE INDEX evidences_claim_idx ON evidences (claim_id, created_at);
CREATE INDEX evidences_source_version_idx ON evidences (source_version_id);

CREATE TABLE page_relations (
    id uuid PRIMARY KEY,
    library_id uuid NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    source_page_id uuid NOT NULL REFERENCES pages(id) ON DELETE RESTRICT,
    target_page_id uuid NOT NULL REFERENCES pages(id) ON DELETE RESTRICT,
    relation_type varchar(40) NOT NULL,
    rationale text,
    confidence numeric(4,3),
    status varchar(20) NOT NULL DEFAULT 'PROPOSED',
    created_by_type varchar(20) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT page_relations_not_self CHECK (source_page_id <> target_page_id),
    CONSTRAINT page_relations_status_check CHECK (status IN ('PROPOSED', 'ACCEPTED', 'REJECTED', 'ARCHIVED')),
    CONSTRAINT page_relations_created_by_type_check CHECK (created_by_type IN ('AI', 'USER', 'IMPORT'))
);

CREATE INDEX page_relations_library_status_idx ON page_relations (library_id, status, created_at DESC);
CREATE INDEX page_relations_source_idx ON page_relations (source_page_id, created_at DESC);
CREATE INDEX page_relations_target_idx ON page_relations (target_page_id, created_at DESC);

CREATE TABLE contradictions (
    id uuid PRIMARY KEY,
    library_id uuid NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    left_claim_id uuid NOT NULL REFERENCES claims(id) ON DELETE RESTRICT,
    right_claim_id uuid NOT NULL REFERENCES claims(id) ON DELETE RESTRICT,
    classification varchar(30) NOT NULL,
    explanation text NOT NULL,
    confidence numeric(4,3),
    status varchar(20) NOT NULL DEFAULT 'OPEN',
    resolution_note text,
    resolved_by uuid REFERENCES users(id) ON DELETE RESTRICT,
    resolved_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT contradictions_not_self CHECK (left_claim_id <> right_claim_id),
    CONSTRAINT contradictions_classification_check CHECK (classification IN ('TRUE_CONFLICT', 'TIME_CHANGE', 'CONDITION_DIFF', 'VIEWPOINT_DIFF', 'POSSIBLE_CONFLICT')),
    CONSTRAINT contradictions_status_check CHECK (status IN ('OPEN', 'ACCEPTED', 'DISMISSED', 'RESOLVED'))
);

CREATE INDEX contradictions_library_status_idx ON contradictions (library_id, status, created_at DESC);
CREATE INDEX contradictions_claims_idx ON contradictions (left_claim_id, right_claim_id);

CREATE FUNCTION reject_knowledge_immutable_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'knowledge evidence and published page versions are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER page_versions_immutable
    BEFORE UPDATE OR DELETE ON page_versions
    FOR EACH ROW EXECUTE FUNCTION reject_knowledge_immutable_mutation();

CREATE TRIGGER evidences_immutable
    BEFORE UPDATE OR DELETE ON evidences
    FOR EACH ROW EXECUTE FUNCTION reject_knowledge_immutable_mutation();

CREATE TRIGGER source_versions_immutable
    BEFORE UPDATE OR DELETE ON source_versions
    FOR EACH ROW EXECUTE FUNCTION reject_knowledge_immutable_mutation();
