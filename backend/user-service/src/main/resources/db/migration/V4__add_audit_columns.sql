-- Bo sung cot audit (created_at/updated_at/created_by/updated_by) cho cac bang
-- chinh. created_by/updated_by la UUID cua users.id nhung KHONG dat FK/JPA
-- relationship - chi la cot tham chieu phang, dung quy uoc chung cua du an.

ALTER TABLE users
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_by UUID;

ALTER TABLE roles
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_by UUID;

ALTER TABLE permissions
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_by UUID;
