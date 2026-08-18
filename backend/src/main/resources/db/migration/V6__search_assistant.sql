CREATE TABLE conversations (
    id uuid PRIMARY KEY,
    library_id uuid NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title text NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT conversations_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX conversations_user_library_updated_idx
    ON conversations (user_id, library_id, updated_at DESC);

CREATE TABLE messages (
    id uuid PRIMARY KEY,
    conversation_id uuid NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role varchar(20) NOT NULL,
    content text NOT NULL,
    answer_mode varchar(30),
    status varchar(20) NOT NULL,
    insufficient_evidence boolean NOT NULL DEFAULT false,
    token_usage integer,
    created_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    CONSTRAINT messages_role_check CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    CONSTRAINT messages_status_check CHECK (status IN ('QUEUED', 'GENERATING', 'COMPLETED', 'FAILED')),
    CONSTRAINT messages_answer_mode_check CHECK (answer_mode IS NULL OR answer_mode IN ('QUICK', 'COMPARE', 'LEARN', 'DECIDE', 'REPORT'))
);

CREATE INDEX messages_conversation_created_idx ON messages (conversation_id, created_at);

CREATE TABLE message_citations (
    id uuid PRIMARY KEY,
    message_id uuid NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    page_version_id uuid REFERENCES page_versions(id) ON DELETE RESTRICT,
    claim_id uuid REFERENCES claims(id) ON DELETE RESTRICT,
    evidence_id uuid REFERENCES evidences(id) ON DELETE RESTRICT,
    source_version_id uuid REFERENCES source_versions(id) ON DELETE RESTRICT,
    citation_order integer NOT NULL,
    quoted_text text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT message_citations_target_check CHECK (
        page_version_id IS NOT NULL OR claim_id IS NOT NULL
        OR evidence_id IS NOT NULL OR source_version_id IS NOT NULL
    ),
    UNIQUE (message_id, citation_order)
);

CREATE INDEX message_citations_message_idx ON message_citations (message_id, citation_order);

CREATE TABLE feedback_events (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    library_id uuid NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    target_type varchar(30) NOT NULL,
    target_id uuid NOT NULL,
    feedback_type varchar(40) NOT NULL,
    comment text,
    scope varchar(20) NOT NULL DEFAULT 'LIBRARY',
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT feedback_events_target_check CHECK (target_type IN ('RELATION', 'ANSWER', 'CLAIM', 'CONTRADICTION', 'SOURCE_TRUST')),
    CONSTRAINT feedback_events_type_check CHECK (feedback_type IN ('HELPFUL', 'NOT_HELPFUL', 'WRONG_TOPIC', 'NOT_CAUSAL', 'WEAK_SOURCE', 'IRRELEVANT', 'DUPLICATE', 'OTHER')),
    CONSTRAINT feedback_events_scope_check CHECK (scope IN ('LIBRARY', 'ACCOUNT'))
);

CREATE INDEX feedback_events_target_idx ON feedback_events (target_type, target_id, created_at DESC);
