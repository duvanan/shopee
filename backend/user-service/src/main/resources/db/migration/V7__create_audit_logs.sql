-- audit_logs (docs/architecture/user-auth-role-permission-redis-refresh-token-design-uuid.md
-- muc 13). old_value/new_value dung TEXT (JSON dang chuoi) thay vi kieu JSON
-- rieng cua Postgres de giu don gian, khop cach Hibernate map String.
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    actor_id UUID NULL REFERENCES users (id) ON DELETE SET NULL,
    actor_email VARCHAR(255) NULL,
    actor_type VARCHAR(30) NOT NULL,

    action VARCHAR(100) NOT NULL,
    module VARCHAR(100) NOT NULL,

    object_type VARCHAR(100) NULL,
    object_id VARCHAR(100) NULL,

    description VARCHAR(2000) NULL,

    old_value TEXT NULL,
    new_value TEXT NULL,

    trace_id VARCHAR(100) NULL,
    status VARCHAR(30) NOT NULL,

    error_message VARCHAR(2000) NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_module ON audit_logs(module);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_object ON audit_logs(object_type, object_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
