-- Bang login_histories (docs/architecture/user-auth-role-permission-redis-refresh-token-design-uuid.md
-- muc 14). Snapshot "email" thay vi "username" vi he thong dang nhap bang
-- email (xem ghi chu trang thai o dau tai lieu do).
CREATE TABLE login_histories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NULL REFERENCES users (id) ON DELETE SET NULL,
    email VARCHAR(255) NULL,

    action VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,

    session_id VARCHAR(100) NULL,
    ip_address VARCHAR(50) NULL,
    user_agent VARCHAR(1000) NULL,

    failure_reason VARCHAR(1000) NULL,
    trace_id VARCHAR(100) NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_login_histories_user_id ON login_histories(user_id);
CREATE INDEX idx_login_histories_action ON login_histories(action);
CREATE INDEX idx_login_histories_created_at ON login_histories(created_at);
