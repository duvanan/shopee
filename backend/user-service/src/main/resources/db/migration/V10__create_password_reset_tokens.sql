-- password_reset_tokens (docs/architecture/user-auth-role-permission-redis-refresh-token-design-uuid.md
-- muc 11). Bo status EXPIRED/REVOKED - "het han" tinh truc tiep tu expires_at
-- luc validate, khong can job rieng cap nhat status; REVOKED chua co nghiep
-- vu nao kich hoat (chua co "admin huy yeu cau reset").
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    requested_ip VARCHAR(50) NULL,
    user_agent VARCHAR(1000) NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_status ON password_reset_tokens(status);

-- Co nguoi dung that (reset password) tu day nen them luon, khong con la cot mo coi.
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMPTZ NULL;
