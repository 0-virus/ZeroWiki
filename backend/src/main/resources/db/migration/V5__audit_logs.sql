CREATE TABLE audit_logs (
    id uuid PRIMARY KEY,
    actor_user_id uuid REFERENCES users(id) ON DELETE RESTRICT,
    library_id uuid REFERENCES libraries(id) ON DELETE RESTRICT,
    event_type varchar(60) NOT NULL,
    resource_type varchar(30) NOT NULL,
    resource_id uuid,
    result varchar(20) NOT NULL,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    request_id varchar(80),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT audit_logs_result_check CHECK (result IN ('SUCCESS', 'DENIED', 'FAILED'))
);

CREATE INDEX audit_logs_library_created_idx ON audit_logs (library_id, created_at DESC);
CREATE INDEX audit_logs_actor_created_idx ON audit_logs (actor_user_id, created_at DESC);

CREATE FUNCTION reject_audit_log_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit logs are append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_immutable
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION reject_audit_log_mutation();
