-- V1 already owns the idempotency record table; this migration only makes
-- the endpoint path part of the lookup contract without creating a duplicate table.
CREATE UNIQUE INDEX idempotency_records_user_path_key_unique_idx
    ON idempotency_records (user_id, request_path, idempotency_key);
