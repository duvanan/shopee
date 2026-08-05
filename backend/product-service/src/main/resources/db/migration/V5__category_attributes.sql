-- Gan thuoc tinh goi y theo danh muc, thay vi bat admin ra thoat tu danh sach
-- toan bo attribute moi lan tao san pham (xem trao doi trong
-- docs/features/03-dynamic-product-catalog.md - van de "nhieu nganh hang,
-- moi nganh 1 bo thuoc tinh khac nhau"). Day chi la GOI Y (default) - luc
-- tao san pham admin van chon/bo duoc, khong khoa cung.

CREATE TABLE category_attributes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    category_id UUID NOT NULL,
    attribute_id UUID NOT NULL,

    is_variant_attribute BOOLEAN NOT NULL DEFAULT FALSE,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_category_attributes_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_category_attributes_attribute FOREIGN KEY (attribute_id) REFERENCES product_attributes(id),
    CONSTRAINT uk_category_attributes UNIQUE (category_id, attribute_id)
);

CREATE INDEX idx_category_attributes_category_id ON category_attributes(category_id);

-- Seed goi y cho danh muc mau (Thoi trang nam da tao thu cong o phien truoc)
-- se khong seed cung o day vi id danh muc do sinh dong luc test, khong co
-- trong migration - admin tu gan qua UI/API moi.
