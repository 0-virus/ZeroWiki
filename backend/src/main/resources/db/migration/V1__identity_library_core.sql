CREATE TABLE users (
    id uuid PRIMARY KEY,
    email varchar(320) NOT NULL,
    password_hash text NOT NULL,
    display_name varchar(80) NOT NULL,
    locale varchar(10) NOT NULL DEFAULT 'ko-KR',
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    deletion_requested_at timestamptz,
    purge_scheduled_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'DELETION_PENDING', 'DELETED', 'SUSPENDED'))
);

CREATE UNIQUE INDEX users_email_lower_unique_idx ON users (lower(email));

CREATE TABLE auth_sessions (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    refresh_token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    ip_hash text,
    user_agent text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX auth_sessions_user_active_idx ON auth_sessions (user_id, revoked_at);

CREATE TABLE idempotency_records (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    idempotency_key varchar(255) NOT NULL,
    request_path text NOT NULL,
    request_body_hash varchar(64) NOT NULL,
    response_status integer,
    response_body jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idempotency_records_expires_idx ON idempotency_records (expires_at);

CREATE TABLE libraries (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name varchar(120) NOT NULL,
    description text,
    template_type varchar(30) NOT NULL,
    visibility varchar(20) NOT NULL DEFAULT 'PRIVATE',
    current_constitution_version_id uuid,
    current_library_version_id uuid,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    revision bigint NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT libraries_visibility_check CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    CONSTRAINT libraries_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETION_PENDING'))
);

CREATE INDEX libraries_owner_status_updated_idx ON libraries (owner_id, status, updated_at DESC);
CREATE INDEX libraries_owner_name_idx ON libraries (owner_id, lower(name));

CREATE TABLE library_constitution_versions (
    id uuid PRIMARY KEY,
    library_id uuid NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    version_no integer NOT NULL,
    constitution_document jsonb NOT NULL,
    created_by_type varchar(20) NOT NULL,
    revision bigint NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (library_id, version_no)
);

ALTER TABLE libraries
    ADD CONSTRAINT libraries_current_constitution_fk
    FOREIGN KEY (current_constitution_version_id)
    REFERENCES library_constitution_versions(id)
    ON DELETE RESTRICT;

CREATE INDEX constitution_versions_library_idx
    ON library_constitution_versions (library_id, version_no DESC);

CREATE FUNCTION reject_constitution_version_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'library constitution versions are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER library_constitution_versions_immutable
    BEFORE UPDATE OR DELETE ON library_constitution_versions
    FOR EACH ROW EXECUTE FUNCTION reject_constitution_version_mutation();

CREATE TABLE library_references (
    id uuid PRIMARY KEY,
    source_library_id uuid NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    target_library_id uuid NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (source_library_id, target_library_id),
    CONSTRAINT library_references_not_self CHECK (source_library_id <> target_library_id)
);

CREATE INDEX library_references_source_idx ON library_references (source_library_id, created_at DESC);
