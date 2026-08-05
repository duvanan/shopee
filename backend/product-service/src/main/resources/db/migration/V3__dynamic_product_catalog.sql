-- Thay the schema san pham don gian (V1/V2) bang model dong: brand, category
-- co cay cha-con, attribute dong, product co nhieu variant, ton kho theo kho.
-- Xem docs/features/03-dynamic-product-catalog.md va
-- docs/architecture/dynamic-product-module-database-design.md.
-- Du an dang o giai doan dev, khong co du lieu production can giu lai nen
-- drop thang thay vi viet script chuyen doi du lieu.

DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS categories CASCADE;

CREATE TABLE brands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,

    logo_url VARCHAR(1000),
    banner_url VARCHAR(1000),

    description TEXT,
    website_url VARCHAR(1000),
    country_code VARCHAR(10),

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NOT NULL DEFAULT 0,

    seo_title VARCHAR(500),
    seo_description VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT uk_brands_code UNIQUE (code),
    CONSTRAINT uk_brands_slug UNIQUE (slug)
);

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    parent_id UUID,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,

    description TEXT,
    image_url VARCHAR(1000),

    level INT NOT NULL DEFAULT 0,
    path VARCHAR(2000),
    sort_order INT NOT NULL DEFAULT 0,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    seo_title VARCHAR(500),
    seo_description VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id),
    CONSTRAINT uk_categories_code UNIQUE (code),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(100) NOT NULL,
    name VARCHAR(500) NOT NULL,
    slug VARCHAR(500) NOT NULL,

    product_type VARCHAR(30) NOT NULL DEFAULT 'SIMPLE',
    brand_id UUID,

    short_description VARCHAR(2000),
    description TEXT,

    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

    min_price NUMERIC(19, 2),
    max_price NUMERIC(19, 2),

    thumbnail_url VARCHAR(1000),

    seo_title VARCHAR(500),
    seo_description VARCHAR(1000),
    seo_keywords VARCHAR(1000),
    canonical_url VARCHAR(1000),

    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    new_until TIMESTAMPTZ,

    sales_count BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,

    rating_average NUMERIC(3, 2) NOT NULL DEFAULT 0,
    rating_count INT NOT NULL DEFAULT 0,

    created_by UUID,
    updated_by UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_products_code UNIQUE (code),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(id)
);

CREATE TABLE product_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    product_id UUID NOT NULL,
    category_id UUID NOT NULL,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_product_categories_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_product_categories_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT uk_product_categories UNIQUE (product_id, category_id)
);

CREATE TABLE product_attributes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,

    data_type VARCHAR(30) NOT NULL,
    input_type VARCHAR(30) NOT NULL,

    unit VARCHAR(50),
    attribute_group VARCHAR(100),

    is_filterable BOOLEAN NOT NULL DEFAULT FALSE,
    is_searchable BOOLEAN NOT NULL DEFAULT FALSE,
    is_comparable BOOLEAN NOT NULL DEFAULT FALSE,
    allow_variant BOOLEAN NOT NULL DEFAULT FALSE,

    validation_rules TEXT,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_product_attributes_code UNIQUE (code)
);

CREATE TABLE attribute_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    attribute_id UUID NOT NULL,

    code VARCHAR(100) NOT NULL,
    value VARCHAR(500) NOT NULL,
    display_value VARCHAR(500),

    color_code VARCHAR(20),
    image_url VARCHAR(1000),
    metadata TEXT,

    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_attribute_values_attribute FOREIGN KEY (attribute_id) REFERENCES product_attributes(id),
    CONSTRAINT uk_attribute_values UNIQUE (attribute_id, code)
);

CREATE TABLE product_attribute_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    product_id UUID NOT NULL,
    attribute_id UUID NOT NULL,

    is_variant_attribute BOOLEAN NOT NULL DEFAULT FALSE,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_filterable BOOLEAN NOT NULL DEFAULT FALSE,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,

    sort_order INT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_product_attribute_configs_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_product_attribute_configs_attribute FOREIGN KEY (attribute_id) REFERENCES product_attributes(id),
    CONSTRAINT uk_product_attribute_configs UNIQUE (product_id, attribute_id)
);

CREATE TABLE product_attribute_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    product_id UUID NOT NULL,
    attribute_id UUID NOT NULL,
    attribute_value_id UUID,

    value_text VARCHAR(2000),
    value_number NUMERIC(19, 4),
    value_boolean BOOLEAN,
    value_date DATE,
    value_json TEXT,

    display_value VARCHAR(2000),
    sort_order INT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_product_attribute_values_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_product_attribute_values_attribute FOREIGN KEY (attribute_id) REFERENCES product_attributes(id),
    CONSTRAINT fk_product_attribute_values_option FOREIGN KEY (attribute_value_id) REFERENCES attribute_values(id)
);

CREATE TABLE product_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    product_id UUID NOT NULL,

    sku VARCHAR(150) NOT NULL,
    barcode VARCHAR(150),

    name VARCHAR(500) NOT NULL,
    variant_signature VARCHAR(1000),

    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    list_price NUMERIC(19, 2) NOT NULL,
    sale_price NUMERIC(19, 2) NOT NULL,
    cost_price NUMERIC(19, 2),

    sale_start_at TIMESTAMPTZ,
    sale_end_at TIMESTAMPTZ,

    weight NUMERIC(19, 4),
    weight_unit VARCHAR(20),

    length NUMERIC(19, 4),
    width NUMERIC(19, 4),
    height NUMERIC(19, 4),
    dimension_unit VARCHAR(20),

    sort_order INT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_product_variants_sku UNIQUE (sku)
);

CREATE TABLE variant_attribute_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    variant_id UUID NOT NULL,
    attribute_id UUID NOT NULL,
    attribute_value_id UUID NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_variant_attribute_values_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT fk_variant_attribute_values_attribute FOREIGN KEY (attribute_id) REFERENCES product_attributes(id),
    CONSTRAINT fk_variant_attribute_values_value FOREIGN KEY (attribute_value_id) REFERENCES attribute_values(id),
    CONSTRAINT uk_variant_attribute_values UNIQUE (variant_id, attribute_id)
);

CREATE TABLE product_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    product_id UUID NOT NULL,
    variant_id UUID,

    file_id UUID,
    image_url VARCHAR(1000) NOT NULL,
    thumbnail_url VARCHAR(1000),

    image_type VARCHAR(30) NOT NULL DEFAULT 'GALLERY',

    alt_text VARCHAR(500),
    title VARCHAR(500),

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_product_images_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

CREATE TABLE warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(1000),

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_warehouses_code UNIQUE (code)
);

CREATE TABLE inventories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    warehouse_id UUID NOT NULL,
    variant_id UUID NOT NULL,

    quantity_on_hand INT NOT NULL DEFAULT 0,
    quantity_reserved INT NOT NULL DEFAULT 0,

    safety_stock INT NOT NULL DEFAULT 0,
    reorder_level INT NOT NULL DEFAULT 0,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_inventories_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT fk_inventories_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT uk_inventories UNIQUE (warehouse_id, variant_id)
);

CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    warehouse_id UUID NOT NULL,
    variant_id UUID NOT NULL,

    transaction_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    quantity_before INT NOT NULL,
    quantity_after INT NOT NULL,

    reference_type VARCHAR(50),
    reference_id UUID,
    note VARCHAR(2000),

    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_inventory_transactions_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT fk_inventory_transactions_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

CREATE INDEX idx_products_brand_id ON products(brand_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_product_type ON products(product_type);
CREATE INDEX idx_products_is_featured ON products(is_featured);
CREATE INDEX idx_products_published_at ON products(published_at);

CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_product_categories_category_id ON product_categories(category_id);
CREATE INDEX idx_product_images_product_id ON product_images(product_id);
CREATE INDEX idx_product_images_variant_id ON product_images(variant_id);
CREATE INDEX idx_attribute_values_attribute_id ON attribute_values(attribute_id);
CREATE INDEX idx_product_attribute_configs_product_id ON product_attribute_configs(product_id);
CREATE INDEX idx_product_attribute_values_product_id ON product_attribute_values(product_id);
CREATE INDEX idx_product_variants_product_id ON product_variants(product_id);
CREATE INDEX idx_variant_attribute_values_variant_id ON variant_attribute_values(variant_id);
CREATE INDEX idx_inventories_variant_id ON inventories(variant_id);
CREATE INDEX idx_inventory_transactions_variant_id ON inventory_transactions(variant_id);
