-- customer_addresses (docs/architecture/user-auth-role-permission-redis-refresh-token-design-uuid.md
-- muc 10). Bo status/deleted_at (xoa cung, khong can vo hieu hoa tam) va
-- latitude/longitude (chua tich hop ban do/geocoding) - them khi co tinh
-- nang tuong ung dung.
CREATE TABLE customer_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    recipient_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,

    province_code VARCHAR(20) NOT NULL,
    province_name VARCHAR(255) NOT NULL,

    district_code VARCHAR(20) NOT NULL,
    district_name VARCHAR(255) NOT NULL,

    ward_code VARCHAR(20) NOT NULL,
    ward_name VARCHAR(255) NOT NULL,

    address_line VARCHAR(1000) NOT NULL,

    address_type VARCHAR(30) NOT NULL DEFAULT 'HOME',
    label VARCHAR(100) NULL,

    is_default BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_customer_addresses_user_id ON customer_addresses(user_id);
CREATE INDEX idx_customer_addresses_is_default ON customer_addresses(user_id, is_default);
