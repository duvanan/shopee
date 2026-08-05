-- Product Type + Attribute Set (xem
-- docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md).
-- Thay the co che "chon attribute thu cong moi lan tao san pham"
-- (product_attribute_configs) bang: chon Product Type -> he thong tu lay
-- Attribute Set mac dinh -> tu sinh form. product_attribute_configs khong
-- con dung, drop luon (du an dang dev, chua co du lieu can giu).

DROP TABLE IF EXISTS product_attribute_configs;

-- 1. product_types (chua co FK toi attribute_sets - them sau khi bang do ton tai)
CREATE TABLE product_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),

    default_attribute_set_id UUID,
    icon_url VARCHAR(1000),

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NOT NULL DEFAULT 0,

    created_by UUID,
    updated_by UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT uk_product_types_code UNIQUE (code)
);

-- 2. attribute_sets
CREATE TABLE attribute_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    version_no INT NOT NULL DEFAULT 1,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,

    created_by UUID,
    updated_by UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT uk_attribute_sets_code UNIQUE (code)
);

-- 3. FK product_types -> attribute_sets, tao sau khi attribute_sets da ton tai
ALTER TABLE product_types
    ADD CONSTRAINT fk_product_types_default_attribute_set
    FOREIGN KEY (default_attribute_set_id) REFERENCES attribute_sets(id);

-- 4. attribute_set_groups
CREATE TABLE attribute_set_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    attribute_set_id UUID NOT NULL,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),

    is_collapsible BOOLEAN NOT NULL DEFAULT TRUE,
    is_default_expanded BOOLEAN NOT NULL DEFAULT TRUE,

    sort_order INT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_attribute_set_groups_set FOREIGN KEY (attribute_set_id) REFERENCES attribute_sets(id),
    CONSTRAINT uk_attribute_set_groups UNIQUE (attribute_set_id, code)
);

-- 5. product_attributes: bo sung field theo thiet ke moi (giu nguyen cot cu
-- is_filterable/is_searchable/is_comparable/allow_variant/attribute_group da
-- co tu truoc, khong xoa de khong vo du lieu dang dung).
ALTER TABLE product_attributes
    ADD COLUMN description VARCHAR(1000),
    ADD COLUMN allow_predefined_values BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN allow_custom_value BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE;

-- 6. attribute_set_attributes - bang quan trong nhat, xac dinh 1 Attribute
-- Set co nhung Attribute nao va cau hinh cu the trong set do.
CREATE TABLE attribute_set_attributes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    attribute_set_id UUID NOT NULL,
    group_id UUID,
    attribute_id UUID NOT NULL,

    label_override VARCHAR(255),
    help_text VARCHAR(1000),
    placeholder VARCHAR(500),

    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_variant_attribute BOOLEAN NOT NULL DEFAULT FALSE,

    is_filterable BOOLEAN NOT NULL DEFAULT FALSE,
    is_searchable BOOLEAN NOT NULL DEFAULT FALSE,
    is_comparable BOOLEAN NOT NULL DEFAULT FALSE,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    is_editable BOOLEAN NOT NULL DEFAULT TRUE,

    default_value_id UUID,
    default_value_text VARCHAR(2000),

    validation_override TEXT,

    sort_order INT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_attribute_set_attributes_set FOREIGN KEY (attribute_set_id) REFERENCES attribute_sets(id),
    CONSTRAINT fk_attribute_set_attributes_group FOREIGN KEY (group_id) REFERENCES attribute_set_groups(id),
    CONSTRAINT fk_attribute_set_attributes_attribute FOREIGN KEY (attribute_id) REFERENCES product_attributes(id),
    CONSTRAINT fk_attribute_set_attributes_default_value FOREIGN KEY (default_value_id) REFERENCES attribute_values(id),
    CONSTRAINT uk_attribute_set_attributes UNIQUE (attribute_set_id, attribute_id)
);

-- 7. products: gan Product Type + Attribute Set (nullable - san pham cu
-- trong du an dev hien tai se khong co, chap nhan duoc).
ALTER TABLE products
    ADD COLUMN product_type_id UUID,
    ADD COLUMN attribute_set_id UUID,
    ADD COLUMN attribute_set_version INT;

ALTER TABLE products
    ADD CONSTRAINT fk_products_product_type FOREIGN KEY (product_type_id) REFERENCES product_types(id),
    ADD CONSTRAINT fk_products_attribute_set FOREIGN KEY (attribute_set_id) REFERENCES attribute_sets(id);

-- 8. product_attribute_values: bo sung cot kieu du lieu con thieu so voi
-- thiet ke moi (value_number hien co dung lam cho ca INTEGER/DECIMAL truoc
-- day - gio tach rieng value_integer, giu value_number cho DECIMAL).
ALTER TABLE product_attribute_values
    ADD COLUMN value_integer BIGINT,
    ADD COLUMN value_datetime TIMESTAMPTZ;

CREATE INDEX idx_product_types_default_attribute_set ON product_types(default_attribute_set_id);
CREATE INDEX idx_attribute_set_groups_set_id ON attribute_set_groups(attribute_set_id);
CREATE INDEX idx_attribute_set_attributes_set_id ON attribute_set_attributes(attribute_set_id);
CREATE INDEX idx_attribute_set_attributes_group_id ON attribute_set_attributes(group_id);
CREATE INDEX idx_products_product_type_id ON products(product_type_id);
CREATE INDEX idx_products_attribute_set_id ON products(attribute_set_id);
