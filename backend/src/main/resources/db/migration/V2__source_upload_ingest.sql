CREATE TABLE upload_sessions (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    file_name varchar(255) NOT NULL,
    media_type varchar(120) NOT NULL,
    byte_size bigint NOT NULL,
    expected_sha256 varchar(64) NOT NULL,
    upload_token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    storage_key text NOT NULL,
    status varchar(24) NOT NULL DEFAULT 'PLANNED',
    uploaded_byte_size bigint,
    uploaded_sha256 varchar(64),
    created_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    CONSTRAINT upload_sessions_size_check CHECK (byte_size > 0),
    CONSTRAINT upload_sessions_status_check CHECK (status IN ('PLANNED', 'UPLOADED', 'COMPLETED', 'CANCELLED', 'BLOCKED_SECRET'))
);

CREATE INDEX upload_sessions_owner_status_idx ON upload_sessions (owner_id, status, created_at DESC);
CREATE INDEX upload_sessions_status_expiry_idx ON upload_sessions (status, expires_at);

CREATE TABLE sources (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    source_type varchar(30) NOT NULL,
    canonical_uri text NOT NULL,
    title text NOT NULL,
    status varchar(24) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT sources_type_check CHECK (source_type IN ('FILE', 'OBSIDIAN', 'NOTION', 'WEB', 'YOUTUBE', 'ARXIV', 'GITHUB', 'NOTE')),
    CONSTRAINT sources_status_check CHECK (status IN ('ACTIVE', 'BLOCKED_SECRET', 'DELETION_PENDING'))
);

CREATE INDEX sources_owner_type_updated_idx ON sources (owner_id, source_type, updated_at DESC);

CREATE TABLE source_versions (
    id uuid PRIMARY KEY,
    source_id uuid NOT NULL REFERENCES sources(id) ON DELETE RESTRICT,
    version_no integer NOT NULL,
    content_hash varchar(64) NOT NULL,
    storage_key text NOT NULL,
    media_type varchar(120) NOT NULL,
    byte_size bigint NOT NULL,
    extracted_text_key text,
    detected_language varchar(10),
    source_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    capture_method varchar(30) NOT NULL,
    captured_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (source_id, version_no),
    CONSTRAINT source_versions_version_check CHECK (version_no > 0),
    CONSTRAINT source_versions_size_check CHECK (byte_size >= 0),
    CONSTRAINT source_versions_capture_check CHECK (capture_method IN ('UPLOAD', 'IMPORT', 'CLIP', 'MANUAL'))
);

CREATE INDEX source_versions_source_created_idx ON source_versions (source_id, version_no DESC);
CREATE INDEX source_versions_hash_idx ON source_versions (content_hash);

CREATE TABLE library_sources (
    library_id uuid NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    source_id uuid NOT NULL REFERENCES sources(id) ON DELETE RESTRICT,
    importance varchar(12) NOT NULL DEFAULT 'NORMAL',
    trust_override numeric(4,3),
    context_note text,
    linked_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (library_id, source_id),
    CONSTRAINT library_sources_importance_check CHECK (importance IN ('LOW', 'NORMAL', 'HIGH', 'CORE')),
    CONSTRAINT library_sources_trust_check CHECK (trust_override IS NULL OR (trust_override >= 0 AND trust_override <= 1))
);

CREATE INDEX library_sources_source_idx ON library_sources (source_id, linked_at DESC);

CREATE TABLE ingest_jobs (
    id uuid PRIMARY KEY,
    library_id uuid NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    requested_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    import_type varchar(24) NOT NULL,
    processing_mode varchar(12) NOT NULL,
    status varchar(24) NOT NULL,
    plan jsonb NOT NULL DEFAULT '{}'::jsonb,
    plan_revision bigint NOT NULL DEFAULT 1,
    estimated_tokens bigint NOT NULL DEFAULT 0,
    actual_tokens bigint,
    estimated_cost numeric(18,6),
    actual_cost numeric(18,6),
    progress_total integer NOT NULL DEFAULT 0,
    progress_completed integer NOT NULL DEFAULT 0,
    failure_code varchar(80),
    failure_message text,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    completed_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ingest_jobs_import_type_check CHECK (import_type IN ('FILES', 'OBSIDIAN_ZIP', 'NOTION', 'URL', 'EXISTING_SOURCES')),
    CONSTRAINT ingest_jobs_processing_mode_check CHECK (processing_mode IN ('FAST', 'STANDARD', 'PRECISE')),
    CONSTRAINT ingest_jobs_status_check CHECK (status IN ('QUEUED', 'SCANNING', 'PLAN_REVIEW', 'PROCESSING', 'QUESTION_REVIEW', 'CHANGE_REVIEW', 'COMPLETED', 'FAILED', 'PAUSED_QUOTA', 'CANCELLED')),
    CONSTRAINT ingest_jobs_progress_check CHECK (progress_total >= 0 AND progress_completed >= 0 AND progress_completed <= progress_total)
);

CREATE INDEX ingest_jobs_library_created_idx ON ingest_jobs (library_id, created_at DESC);
CREATE INDEX ingest_jobs_worker_status_idx ON ingest_jobs (status, created_at);

CREATE TABLE ingest_items (
    id uuid PRIMARY KEY,
    ingest_job_id uuid NOT NULL REFERENCES ingest_jobs(id) ON DELETE CASCADE,
    source_version_id uuid NOT NULL REFERENCES source_versions(id) ON DELETE RESTRICT,
    status varchar(16) NOT NULL DEFAULT 'PENDING',
    topic_hints jsonb NOT NULL DEFAULT '[]'::jsonb,
    duplicate_of_source_id uuid REFERENCES sources(id) ON DELETE RESTRICT,
    sensitivity_flags jsonb NOT NULL DEFAULT '[]'::jsonb,
    retry_count integer NOT NULL DEFAULT 0,
    failure_code varchar(80),
    failure_message text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ingest_items_status_check CHECK (status IN ('PENDING', 'SCANNED', 'PROCESSING', 'DONE', 'HELD', 'FAILED', 'SKIPPED')),
    CONSTRAINT ingest_items_retry_check CHECK (retry_count >= 0)
);

CREATE INDEX ingest_items_job_status_idx ON ingest_items (ingest_job_id, status, created_at);

CREATE TABLE job_questions (
    id uuid PRIMARY KEY,
    ingest_job_id uuid NOT NULL REFERENCES ingest_jobs(id) ON DELETE CASCADE,
    ingest_item_id uuid REFERENCES ingest_items(id) ON DELETE CASCADE,
    question text NOT NULL,
    answer text,
    status varchar(12) NOT NULL DEFAULT 'OPEN',
    created_at timestamptz NOT NULL DEFAULT now(),
    answered_at timestamptz,
    CONSTRAINT job_questions_status_check CHECK (status IN ('OPEN', 'ANSWERED', 'SKIPPED'))
);

CREATE INDEX job_questions_job_status_idx ON job_questions (ingest_job_id, status, created_at);
