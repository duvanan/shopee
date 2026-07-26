-- Bo sung cot audit (created_at/updated_at/created_by/updated_by) cho
-- categories va products. created_by/updated_by la UUID cua user (ben
-- user-service) nhung KHONG dat FK xuyen service - chi la cot tham chieu phang.

ALTER TABLE categories
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_by UUID;

ALTER TABLE products
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_by UUID;
