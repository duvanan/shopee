-- CustomerAddress entity ke thua Auditable (created_at/updated_at/created_by/
-- updated_by) nhung V9 chi tao created_at/updated_at - bo sot created_by/
-- updated_by, gay SchemaManagementException luc validate. Vá lai cho khop.
ALTER TABLE customer_addresses
    ADD COLUMN created_by UUID NULL,
    ADD COLUMN updated_by UUID NULL;
