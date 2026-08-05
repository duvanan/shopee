# Thiết kế Module Product Động cho hệ thống thương mại điện tử

## 1. Mục tiêu

Tài liệu này mô tả thiết kế database và nghiệp vụ cho module quản lý sản phẩm động, hỗ trợ:

- Sản phẩm đơn giản.
- Sản phẩm có nhiều biến thể.
- Thuộc tính động theo từng loại sản phẩm.
- Giá trị thuộc tính dạng chọn sẵn hoặc nhập tự do.
- SKU, giá, tồn kho, kích thước và ảnh theo từng biến thể.
- Một sản phẩm thuộc nhiều danh mục.
- Quản lý thương hiệu.
- SEO.
- Sản phẩm nổi bật, sản phẩm mới và sản phẩm bán chạy.

Tài liệu được dùng làm đầu vào cho Claude Code để sinh:

- Liquibase migration.
- Entity.
- Repository.
- Service.
- Controller.
- Request DTO.
- Response DTO.
- Mapper.
- Validation.
- Unit test.
- Integration test.

---

# 2. Nguyên tắc thiết kế

Sản phẩm được chia thành ba lớp:

```text
Product
→ Thông tin chung của sản phẩm

Attribute
→ Thuộc tính động như màu sắc, kích thước, RAM, chất liệu

Variant
→ Phiên bản cụ thể có thể bán, có SKU, giá và tồn kho riêng
```

Ví dụ:

```text
Sản phẩm: Áo thun nam

Thuộc tính tạo biến thể:
- Màu sắc: Đen, Trắng
- Kích thước: M, L

Thuộc tính mô tả:
- Chất liệu: Cotton
- Xuất xứ: Việt Nam
```

Các biến thể:

```text
Áo thun - Đen - M
Áo thun - Đen - L
Áo thun - Trắng - M
Áo thun - Trắng - L
```

Mỗi biến thể có:

```text
SKU riêng
Giá riêng
Ảnh riêng
Khối lượng riêng
Kích thước riêng
Tồn kho riêng
Trạng thái riêng
```

---

# 3. Danh sách bảng

```text
products
brands
categories
product_categories
product_images
product_attributes
attribute_values
product_attribute_configs
product_attribute_values
product_variants
variant_attribute_values
warehouses
inventories
inventory_transactions
```

Quan hệ tổng quát:

```text
brands
  └── products

products
  ├── product_categories ── categories
  ├── product_images
  ├── product_attribute_configs ── product_attributes
  ├── product_attribute_values ── product_attributes
  └── product_variants
        ├── variant_attribute_values
        ├── product_images
        └── inventories ── warehouses
```

---

# 4. Chuẩn UUID

Toàn bộ primary key và foreign key dùng UUID.

Java:

```java
import java.util.UUID;

private UUID id;
```

MariaDB/MySQL:

```sql
id CHAR(36) PRIMARY KEY
```

PostgreSQL:

```sql
id UUID PRIMARY KEY
```

Không dùng `AUTO_INCREMENT`.

Trường `version` vẫn dùng `BIGINT` để phục vụ optimistic locking.

---

# 5. Bảng `products`

## 5.1. Mục đích

Lưu thông tin chung của sản phẩm.

Không lưu:

- SKU chi tiết.
- Giá từng biến thể.
- Tồn kho từng biến thể.
- Màu sắc hoặc kích thước cố định.

Các thông tin này nằm ở bảng `product_variants` và các bảng thuộc tính.

## 5.2. Field chi tiết

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `code` | VARCHAR(100) | Có | Mã sản phẩm |
| `name` | VARCHAR(500) | Có | Tên sản phẩm |
| `slug` | VARCHAR(500) | Có | URL thân thiện |
| `product_type` | VARCHAR(30) | Có | SIMPLE hoặc VARIABLE |
| `brand_id` | UUID | Không | Thương hiệu |
| `short_description` | VARCHAR(2000) | Không | Mô tả ngắn |
| `description` | LONGTEXT | Không | Mô tả chi tiết |
| `status` | VARCHAR(30) | Có | Trạng thái sản phẩm |
| `min_price` | DECIMAL(19,2) | Không | Giá nhỏ nhất |
| `max_price` | DECIMAL(19,2) | Không | Giá lớn nhất |
| `thumbnail_url` | VARCHAR(1000) | Không | Ảnh đại diện cache |
| `seo_title` | VARCHAR(500) | Không | SEO title |
| `seo_description` | VARCHAR(1000) | Không | SEO description |
| `seo_keywords` | VARCHAR(1000) | Không | Từ khóa SEO |
| `canonical_url` | VARCHAR(1000) | Không | Canonical URL |
| `is_featured` | BOOLEAN | Có | Sản phẩm nổi bật |
| `published_at` | DATETIME | Không | Thời điểm xuất bản |
| `new_until` | DATETIME | Không | Là sản phẩm mới đến ngày nào |
| `sales_count` | BIGINT | Có | Tổng số lượng bán cache |
| `view_count` | BIGINT | Có | Lượt xem |
| `rating_average` | DECIMAL(3,2) | Có | Điểm đánh giá trung bình |
| `rating_count` | INT | Có | Số lượt đánh giá |
| `created_by` | UUID | Không | Người tạo |
| `updated_by` | UUID | Không | Người cập nhật |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |
| `deleted_at` | DATETIME | Không | Xóa mềm |
| `version` | BIGINT | Có | Optimistic locking |

## 5.3. SQL

```sql
CREATE TABLE products (
    id CHAR(36) PRIMARY KEY,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(500) NOT NULL,
    slug VARCHAR(500) NOT NULL,

    product_type VARCHAR(30) NOT NULL DEFAULT 'SIMPLE',
    brand_id CHAR(36) NULL,

    short_description VARCHAR(2000) NULL,
    description LONGTEXT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

    min_price DECIMAL(19, 2) NULL,
    max_price DECIMAL(19, 2) NULL,

    thumbnail_url VARCHAR(1000) NULL,

    seo_title VARCHAR(500) NULL,
    seo_description VARCHAR(1000) NULL,
    seo_keywords VARCHAR(1000) NULL,
    canonical_url VARCHAR(1000) NULL,

    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    published_at DATETIME(6) NULL,
    new_until DATETIME(6) NULL,

    sales_count BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,

    rating_average DECIMAL(3, 2) NOT NULL DEFAULT 0,
    rating_count INT NOT NULL DEFAULT 0,

    created_by CHAR(36) NULL,
    updated_by CHAR(36) NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_products_code UNIQUE (code),
    CONSTRAINT uk_products_slug UNIQUE (slug),

    CONSTRAINT fk_products_brand
        FOREIGN KEY (brand_id) REFERENCES brands(id)
);

CREATE INDEX idx_products_brand_id ON products(brand_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_product_type ON products(product_type);
CREATE INDEX idx_products_is_featured ON products(is_featured);
CREATE INDEX idx_products_published_at ON products(published_at);
```

## 5.4. Enum

```java
public enum ProductType {
    SIMPLE,
    VARIABLE
}
```

```java
public enum ProductStatus {
    DRAFT,
    PENDING_APPROVAL,
    ACTIVE,
    INACTIVE,
    OUT_OF_STOCK,
    DISCONTINUED,
    ARCHIVED
}
```

## 5.5. Quy tắc

- `code` và `slug` phải duy nhất.
- Tiền dùng `BigDecimal`.
- Không dùng `double` hoặc `float`.
- `min_price` và `max_price` được tính từ các variant đang hoạt động.
- `is_featured` là cấu hình thủ công.
- Sản phẩm mới xác định bằng `published_at` hoặc `new_until`.
- Sản phẩm bán chạy xác định bằng `sales_count` hoặc báo cáo đơn hàng.
- Sản phẩm đã phát sinh đơn hàng chỉ được xóa mềm.

---

# 6. Bảng `brands`

## 6.1. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `code` | VARCHAR(100) | Có | Mã thương hiệu |
| `name` | VARCHAR(255) | Có | Tên thương hiệu |
| `slug` | VARCHAR(255) | Có | Slug |
| `logo_url` | VARCHAR(1000) | Không | Logo |
| `banner_url` | VARCHAR(1000) | Không | Banner |
| `description` | LONGTEXT | Không | Mô tả |
| `website_url` | VARCHAR(1000) | Không | Website |
| `country_code` | VARCHAR(10) | Không | Quốc gia |
| `status` | VARCHAR(30) | Có | Trạng thái |
| `sort_order` | INT | Có | Thứ tự |
| `seo_title` | VARCHAR(500) | Không | SEO title |
| `seo_description` | VARCHAR(1000) | Không | SEO description |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |
| `deleted_at` | DATETIME | Không | Xóa mềm |

## 6.2. SQL

```sql
CREATE TABLE brands (
    id CHAR(36) PRIMARY KEY,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,

    logo_url VARCHAR(1000) NULL,
    banner_url VARCHAR(1000) NULL,

    description LONGTEXT NULL,
    website_url VARCHAR(1000) NULL,
    country_code VARCHAR(10) NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NOT NULL DEFAULT 0,

    seo_title VARCHAR(500) NULL,
    seo_description VARCHAR(1000) NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    CONSTRAINT uk_brands_code UNIQUE (code),
    CONSTRAINT uk_brands_slug UNIQUE (slug)
);
```

---

# 7. Bảng `categories`

## 7.1. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `parent_id` | UUID | Không | Danh mục cha |
| `code` | VARCHAR(100) | Có | Mã danh mục |
| `name` | VARCHAR(255) | Có | Tên |
| `slug` | VARCHAR(255) | Có | Slug |
| `description` | LONGTEXT | Không | Mô tả |
| `image_url` | VARCHAR(1000) | Không | Ảnh |
| `level` | INT | Có | Cấp danh mục |
| `path` | VARCHAR(2000) | Không | Đường dẫn cây |
| `sort_order` | INT | Có | Thứ tự |
| `status` | VARCHAR(30) | Có | Trạng thái |
| `seo_title` | VARCHAR(500) | Không | SEO title |
| `seo_description` | VARCHAR(1000) | Không | SEO description |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |
| `deleted_at` | DATETIME | Không | Xóa mềm |

## 7.2. SQL

```sql
CREATE TABLE categories (
    id CHAR(36) PRIMARY KEY,

    parent_id CHAR(36) NULL,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,

    description LONGTEXT NULL,
    image_url VARCHAR(1000) NULL,

    level INT NOT NULL DEFAULT 0,
    path VARCHAR(2000) NULL,
    sort_order INT NOT NULL DEFAULT 0,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    seo_title VARCHAR(500) NULL,
    seo_description VARCHAR(1000) NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories(id),

    CONSTRAINT uk_categories_code UNIQUE (code),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);
```

---

# 8. Bảng `product_categories`

## 8.1. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `product_id` | UUID | Có | Sản phẩm |
| `category_id` | UUID | Có | Danh mục |
| `is_primary` | BOOLEAN | Có | Danh mục chính |
| `sort_order` | INT | Có | Thứ tự |
| `created_at` | DATETIME | Có | Ngày gán |

## 8.2. SQL

```sql
CREATE TABLE product_categories (
    id CHAR(36) PRIMARY KEY,

    product_id CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,

    created_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_product_categories_product
        FOREIGN KEY (product_id) REFERENCES products(id),

    CONSTRAINT fk_product_categories_category
        FOREIGN KEY (category_id) REFERENCES categories(id),

    CONSTRAINT uk_product_categories
        UNIQUE (product_id, category_id)
);
```

Quy tắc:

- Mỗi sản phẩm có tối đa một danh mục chính.
- Không gán trùng cùng một danh mục.

---

# 9. Bảng `product_images`

## 9.1. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `product_id` | UUID | Có | Sản phẩm |
| `variant_id` | UUID | Không | Variant |
| `file_id` | UUID | Không | File trong file service |
| `image_url` | VARCHAR(1000) | Có | URL ảnh |
| `thumbnail_url` | VARCHAR(1000) | Không | Thumbnail |
| `image_type` | VARCHAR(30) | Có | COVER, GALLERY, VARIANT |
| `alt_text` | VARCHAR(500) | Không | Alt SEO |
| `title` | VARCHAR(500) | Không | Tiêu đề |
| `is_primary` | BOOLEAN | Có | Ảnh chính |
| `sort_order` | INT | Có | Thứ tự |
| `status` | VARCHAR(30) | Có | Trạng thái |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |

## 9.2. SQL

```sql
CREATE TABLE product_images (
    id CHAR(36) PRIMARY KEY,

    product_id CHAR(36) NOT NULL,
    variant_id CHAR(36) NULL,

    file_id CHAR(36) NULL,
    image_url VARCHAR(1000) NOT NULL,
    thumbnail_url VARCHAR(1000) NULL,

    image_type VARCHAR(30) NOT NULL DEFAULT 'GALLERY',

    alt_text VARCHAR(500) NULL,
    title VARCHAR(500) NULL,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id) REFERENCES products(id),

    CONSTRAINT fk_product_images_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);
```

Quy ước:

```text
variant_id = null
→ ảnh chung của sản phẩm

variant_id có giá trị
→ ảnh riêng của variant
```

---

# 10. Bảng `product_attributes`

## 10.1. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `code` | VARCHAR(100) | Có | Mã thuộc tính |
| `name` | VARCHAR(255) | Có | Tên |
| `data_type` | VARCHAR(30) | Có | Kiểu dữ liệu |
| `input_type` | VARCHAR(30) | Có | Kiểu input UI |
| `unit` | VARCHAR(50) | Không | Đơn vị |
| `attribute_group` | VARCHAR(100) | Không | Nhóm |
| `is_filterable` | BOOLEAN | Có | Dùng lọc |
| `is_searchable` | BOOLEAN | Có | Dùng tìm kiếm |
| `is_comparable` | BOOLEAN | Có | Dùng so sánh |
| `allow_variant` | BOOLEAN | Có | Cho phép tạo variant |
| `validation_rules` | LONGTEXT | Không | JSON validation |
| `status` | VARCHAR(30) | Có | Trạng thái |
| `sort_order` | INT | Có | Thứ tự |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |

## 10.2. SQL

```sql
CREATE TABLE product_attributes (
    id CHAR(36) PRIMARY KEY,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,

    data_type VARCHAR(30) NOT NULL,
    input_type VARCHAR(30) NOT NULL,

    unit VARCHAR(50) NULL,
    attribute_group VARCHAR(100) NULL,

    is_filterable BOOLEAN NOT NULL DEFAULT FALSE,
    is_searchable BOOLEAN NOT NULL DEFAULT FALSE,
    is_comparable BOOLEAN NOT NULL DEFAULT FALSE,
    allow_variant BOOLEAN NOT NULL DEFAULT FALSE,

    validation_rules LONGTEXT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NOT NULL DEFAULT 0,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT uk_product_attributes_code UNIQUE (code)
);
```

## 10.3. Enum

```java
public enum AttributeDataType {
    STRING,
    NUMBER,
    DECIMAL,
    BOOLEAN,
    DATE,
    DATETIME,
    JSON
}
```

```java
public enum AttributeInputType {
    TEXT,
    TEXTAREA,
    NUMBER,
    SELECT,
    MULTI_SELECT,
    RADIO,
    CHECKBOX,
    COLOR,
    DATE
}
```

---

# 11. Bảng `attribute_values`

## 11.1. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `attribute_id` | UUID | Có | Thuộc tính |
| `code` | VARCHAR(100) | Có | Mã giá trị |
| `value` | VARCHAR(500) | Có | Giá trị lưu |
| `display_value` | VARCHAR(500) | Không | Giá trị hiển thị |
| `color_code` | VARCHAR(20) | Không | Mã màu |
| `image_url` | VARCHAR(1000) | Không | Ảnh minh họa |
| `metadata` | LONGTEXT | Không | JSON mở rộng |
| `sort_order` | INT | Có | Thứ tự |
| `status` | VARCHAR(30) | Có | Trạng thái |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |

## 11.2. SQL

```sql
CREATE TABLE attribute_values (
    id CHAR(36) PRIMARY KEY,

    attribute_id CHAR(36) NOT NULL,

    code VARCHAR(100) NOT NULL,
    value VARCHAR(500) NOT NULL,
    display_value VARCHAR(500) NULL,

    color_code VARCHAR(20) NULL,
    image_url VARCHAR(1000) NULL,
    metadata LONGTEXT NULL,

    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_attribute_values_attribute
        FOREIGN KEY (attribute_id) REFERENCES product_attributes(id),

    CONSTRAINT uk_attribute_values
        UNIQUE (attribute_id, code)
);
```

---

# 12. Bảng `product_attribute_configs`

## 12.1. Mục đích

Xác định product sử dụng attribute nào và attribute nào dùng tạo variant.

## 12.2. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `product_id` | UUID | Có | Sản phẩm |
| `attribute_id` | UUID | Có | Thuộc tính |
| `is_variant_attribute` | BOOLEAN | Có | Tạo variant |
| `is_required` | BOOLEAN | Có | Bắt buộc |
| `is_filterable` | BOOLEAN | Có | Dùng lọc |
| `is_visible` | BOOLEAN | Có | Hiển thị |
| `sort_order` | INT | Có | Thứ tự |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |

## 12.3. SQL

```sql
CREATE TABLE product_attribute_configs (
    id CHAR(36) PRIMARY KEY,

    product_id CHAR(36) NOT NULL,
    attribute_id CHAR(36) NOT NULL,

    is_variant_attribute BOOLEAN NOT NULL DEFAULT FALSE,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_filterable BOOLEAN NOT NULL DEFAULT FALSE,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,

    sort_order INT NOT NULL DEFAULT 0,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_product_attribute_configs_product
        FOREIGN KEY (product_id) REFERENCES products(id),

    CONSTRAINT fk_product_attribute_configs_attribute
        FOREIGN KEY (attribute_id) REFERENCES product_attributes(id),

    CONSTRAINT uk_product_attribute_configs
        UNIQUE (product_id, attribute_id)
);
```

---

# 13. Bảng `product_attribute_values`

## 13.1. Mục đích

Lưu giá trị thuộc tính chung của product, không dùng để xác định variant.

Ví dụ:

```text
Chất liệu = Cotton
Xuất xứ = Việt Nam
Bảo hành = 12 tháng
```

## 13.2. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `product_id` | UUID | Có | Sản phẩm |
| `attribute_id` | UUID | Có | Thuộc tính |
| `attribute_value_id` | UUID | Không | Giá trị chọn sẵn |
| `value_text` | VARCHAR(2000) | Không | Text |
| `value_number` | DECIMAL(19,4) | Không | Số |
| `value_boolean` | BOOLEAN | Không | Boolean |
| `value_date` | DATE | Không | Ngày |
| `value_json` | LONGTEXT | Không | JSON |
| `display_value` | VARCHAR(2000) | Không | Giá trị hiển thị |
| `sort_order` | INT | Có | Thứ tự |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |

## 13.3. SQL

```sql
CREATE TABLE product_attribute_values (
    id CHAR(36) PRIMARY KEY,

    product_id CHAR(36) NOT NULL,
    attribute_id CHAR(36) NOT NULL,
    attribute_value_id CHAR(36) NULL,

    value_text VARCHAR(2000) NULL,
    value_number DECIMAL(19, 4) NULL,
    value_boolean BOOLEAN NULL,
    value_date DATE NULL,
    value_json LONGTEXT NULL,

    display_value VARCHAR(2000) NULL,
    sort_order INT NOT NULL DEFAULT 0,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_product_attribute_values_product
        FOREIGN KEY (product_id) REFERENCES products(id),

    CONSTRAINT fk_product_attribute_values_attribute
        FOREIGN KEY (attribute_id) REFERENCES product_attributes(id),

    CONSTRAINT fk_product_attribute_values_option
        FOREIGN KEY (attribute_value_id) REFERENCES attribute_values(id)
);
```

Quy tắc: chỉ dùng một field giá trị phù hợp với `data_type`.

---

# 14. Bảng `product_variants`

## 14.1. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `product_id` | UUID | Có | Sản phẩm |
| `sku` | VARCHAR(150) | Có | SKU duy nhất |
| `barcode` | VARCHAR(150) | Không | Mã vạch |
| `name` | VARCHAR(500) | Có | Tên variant |
| `variant_signature` | VARCHAR(1000) | Không | Chuỗi tổ hợp |
| `is_default` | BOOLEAN | Có | Variant mặc định |
| `status` | VARCHAR(30) | Có | Trạng thái |
| `list_price` | DECIMAL(19,2) | Có | Giá niêm yết |
| `sale_price` | DECIMAL(19,2) | Có | Giá bán |
| `cost_price` | DECIMAL(19,2) | Không | Giá vốn |
| `sale_start_at` | DATETIME | Không | Bắt đầu sale |
| `sale_end_at` | DATETIME | Không | Kết thúc sale |
| `weight` | DECIMAL(19,4) | Không | Khối lượng |
| `weight_unit` | VARCHAR(20) | Không | G, KG |
| `length` | DECIMAL(19,4) | Không | Dài |
| `width` | DECIMAL(19,4) | Không | Rộng |
| `height` | DECIMAL(19,4) | Không | Cao |
| `dimension_unit` | VARCHAR(20) | Không | CM, MM |
| `sort_order` | INT | Có | Thứ tự |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |
| `deleted_at` | DATETIME | Không | Xóa mềm |
| `version` | BIGINT | Có | Optimistic locking |

## 14.2. SQL

```sql
CREATE TABLE product_variants (
    id CHAR(36) PRIMARY KEY,

    product_id CHAR(36) NOT NULL,

    sku VARCHAR(150) NOT NULL,
    barcode VARCHAR(150) NULL,

    name VARCHAR(500) NOT NULL,
    variant_signature VARCHAR(1000) NULL,

    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    list_price DECIMAL(19, 2) NOT NULL,
    sale_price DECIMAL(19, 2) NOT NULL,
    cost_price DECIMAL(19, 2) NULL,

    sale_start_at DATETIME(6) NULL,
    sale_end_at DATETIME(6) NULL,

    weight DECIMAL(19, 4) NULL,
    weight_unit VARCHAR(20) NULL,

    length DECIMAL(19, 4) NULL,
    width DECIMAL(19, 4) NULL,
    height DECIMAL(19, 4) NULL,
    dimension_unit VARCHAR(20) NULL,

    sort_order INT NOT NULL DEFAULT 0,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_product_variants_product
        FOREIGN KEY (product_id) REFERENCES products(id),

    CONSTRAINT uk_product_variants_sku UNIQUE (sku)
);
```

## 14.3. Variant signature

Ví dụ:

```text
COLOR=BLACK|SIZE=M
COLOR=BLACK|SIZE=L
```

Có thể hash tổ hợp đã sắp xếp bằng SHA-256.

---

# 15. Bảng `variant_attribute_values`

## 15.1. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `variant_id` | UUID | Có | Variant |
| `attribute_id` | UUID | Có | Thuộc tính |
| `attribute_value_id` | UUID | Có | Giá trị |
| `created_at` | DATETIME | Có | Ngày tạo |

## 15.2. SQL

```sql
CREATE TABLE variant_attribute_values (
    id CHAR(36) PRIMARY KEY,

    variant_id CHAR(36) NOT NULL,
    attribute_id CHAR(36) NOT NULL,
    attribute_value_id CHAR(36) NOT NULL,

    created_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_variant_attribute_values_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants(id),

    CONSTRAINT fk_variant_attribute_values_attribute
        FOREIGN KEY (attribute_id) REFERENCES product_attributes(id),

    CONSTRAINT fk_variant_attribute_values_value
        FOREIGN KEY (attribute_value_id) REFERENCES attribute_values(id),

    CONSTRAINT uk_variant_attribute_values
        UNIQUE (variant_id, attribute_id)
);
```

---

# 16. Bảng `warehouses`

```sql
CREATE TABLE warehouses (
    id CHAR(36) PRIMARY KEY,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(1000) NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT uk_warehouses_code UNIQUE (code)
);
```

---

# 17. Bảng `inventories`

## 17.1. Công thức

```text
quantity_available =
quantity_on_hand - quantity_reserved
```

## 17.2. SQL

```sql
CREATE TABLE inventories (
    id CHAR(36) PRIMARY KEY,

    warehouse_id CHAR(36) NOT NULL,
    variant_id CHAR(36) NOT NULL,

    quantity_on_hand INT NOT NULL DEFAULT 0,
    quantity_reserved INT NOT NULL DEFAULT 0,

    safety_stock INT NOT NULL DEFAULT 0,
    reorder_level INT NOT NULL DEFAULT 0,

    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_inventories_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants(id),

    CONSTRAINT fk_inventories_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),

    CONSTRAINT uk_inventories
        UNIQUE (warehouse_id, variant_id)
);
```

Quy tắc:

- Không để tồn âm.
- `quantity_reserved <= quantity_on_hand`.
- Mọi thay đổi phải có transaction log.

---

# 18. Bảng `inventory_transactions`

## 18.1. Field

| Field | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| `id` | UUID | Khóa chính |
| `warehouse_id` | UUID | Kho |
| `variant_id` | UUID | Variant |
| `transaction_type` | VARCHAR(50) | Loại giao dịch |
| `quantity` | INT | Số lượng thay đổi |
| `quantity_before` | INT | Tồn trước |
| `quantity_after` | INT | Tồn sau |
| `reference_type` | VARCHAR(50) | ORDER, IMPORT... |
| `reference_id` | UUID | ID tham chiếu |
| `note` | VARCHAR(2000) | Ghi chú |
| `created_by` | UUID | Người thực hiện |
| `created_at` | DATETIME | Ngày tạo |

## 18.2. Enum

```java
public enum InventoryTransactionType {
    IMPORT,
    EXPORT,
    ORDER_RESERVE,
    ORDER_RELEASE,
    ORDER_CONFIRM,
    ORDER_CANCEL,
    RETURN,
    ADJUSTMENT
}
```

---

# 19. Luồng tạo sản phẩm động

```text
Bước 1: Nhập thông tin chung
Bước 2: Chọn thuộc tính
Bước 3: Chọn thuộc tính tạo variant
Bước 4: Chọn giá trị thuộc tính
Bước 5: Sinh tổ hợp variant
Bước 6: Nhập SKU, giá, ảnh, kích thước, tồn kho
Bước 7: Nhập thuộc tính mô tả chung
Bước 8: Preview
Bước 9: Lưu nháp hoặc xuất bản
```

---

# 20. Request API tạo sản phẩm

```json
{
  "code": "TSHIRT-001",
  "name": "Áo thun nam Basic",
  "slug": "ao-thun-nam-basic",
  "productType": "VARIABLE",
  "brandId": "550e8400-e29b-41d4-a716-446655440000",
  "categoryIds": [
    "be09eaf1-4f8a-4929-a712-c2fa1f531f89"
  ],
  "shortDescription": "Áo thun nam chất liệu cotton",
  "description": "<p>Mô tả chi tiết</p>",
  "status": "DRAFT",
  "isFeatured": false,
  "seo": {
    "title": "Áo thun nam Basic",
    "description": "Áo thun nam cotton cao cấp",
    "keywords": "áo thun nam, áo cotton"
  },
  "attributeConfigs": [
    {
      "attributeId": "color-uuid",
      "isVariantAttribute": true,
      "isRequired": true,
      "isFilterable": true,
      "isVisible": true,
      "valueIds": [
        "black-uuid",
        "white-uuid"
      ]
    },
    {
      "attributeId": "size-uuid",
      "isVariantAttribute": true,
      "isRequired": true,
      "isFilterable": true,
      "isVisible": true,
      "valueIds": [
        "size-m-uuid",
        "size-l-uuid"
      ]
    },
    {
      "attributeId": "material-uuid",
      "isVariantAttribute": false,
      "isRequired": true,
      "isVisible": true,
      "valueText": "100% Cotton"
    }
  ],
  "variants": [
    {
      "sku": "TSHIRT-001-BLACK-M",
      "name": "Đen / M",
      "attributeValueIds": [
        "black-uuid",
        "size-m-uuid"
      ],
      "listPrice": 250000,
      "salePrice": 199000,
      "costPrice": 120000,
      "weight": 250,
      "weightUnit": "G",
      "length": 30,
      "width": 20,
      "height": 3,
      "dimensionUnit": "CM",
      "initialStocks": [
        {
          "warehouseId": "warehouse-uuid",
          "quantity": 100
        }
      ]
    }
  ]
}
```

---

# 21. API đề xuất

## Product

```text
GET    /api/v1/public/products
GET    /api/v1/public/products/{slug}

GET    /api/v1/admin/products
GET    /api/v1/admin/products/{id}
POST   /api/v1/admin/products
PUT    /api/v1/admin/products/{id}
DELETE /api/v1/admin/products/{id}

POST   /api/v1/admin/products/{id}/publish
POST   /api/v1/admin/products/{id}/deactivate
POST   /api/v1/admin/products/{id}/clone
```

## Variant

```text
GET    /api/v1/admin/products/{productId}/variants
POST   /api/v1/admin/products/{productId}/variants
PUT    /api/v1/admin/products/{productId}/variants/{variantId}
DELETE /api/v1/admin/products/{productId}/variants/{variantId}
```

## Attribute

```text
GET    /api/v1/admin/product-attributes
POST   /api/v1/admin/product-attributes
PUT    /api/v1/admin/product-attributes/{id}

GET    /api/v1/admin/product-attributes/{id}/values
POST   /api/v1/admin/product-attributes/{id}/values
PUT    /api/v1/admin/product-attributes/{id}/values/{valueId}
```

## Variant generator

```text
POST /api/v1/admin/products/variants/generate-preview
```

---

# 22. Package structure

```text
com.example.ecommerce.catalog
├── product
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   ├── service
│   ├── mapper
│   └── validator
├── variant
├── attribute
├── category
├── brand
├── image
└── inventory
```

---

# 23. Quy tắc nghiệp vụ bắt buộc

1. Không lưu tồn kho trực tiếp trong `products`.
2. Không lưu màu, size, RAM thành cột cố định.
3. SKU, giá và kích thước nằm ở `product_variants`.
4. SIMPLE product vẫn có một default variant.
5. SKU duy nhất toàn hệ thống.
6. Không cho hai variant cùng tổ hợp.
7. Chỉ attribute `allow_variant = true` mới tạo variant.
8. Một variant không có hai value cùng attribute.
9. Không xóa cứng attribute value đang được sử dụng.
10. Giá không âm.
11. Tồn kho không âm.
12. `quantity_reserved` không lớn hơn `quantity_on_hand`.
13. Mỗi product chỉ có một default variant.
14. Mỗi product chỉ có một primary category.
15. Product đã tham gia order không được xóa cứng.
16. Giới hạn tối đa 100–200 variants/product.

---

# 24. Luồng tạo product trong transaction

```text
1. Validate code và slug.
2. Validate brand.
3. Validate category.
4. Validate attributes.
5. Validate attribute values.
6. Validate variant combinations.
7. Tạo product.
8. Tạo product_categories.
9. Tạo product_attribute_configs.
10. Tạo product_attribute_values.
11. Tạo product_variants.
12. Tạo variant_attribute_values.
13. Tạo product_images.
14. Tạo initial inventory.
15. Tạo inventory transaction IMPORT.
16. Tính min_price và max_price.
17. Commit transaction.
```

Không upload file trong transaction database dài.

---

# 25. Quy tắc coding cho Claude Code

1. Dùng UUID cho toàn bộ ID.
2. Dùng `BigDecimal` cho tiền.
3. Không dùng entity làm response DTO.
4. Request và response DTO tách riêng.
5. Controller không chứa business logic.
6. Service chứa nghiệp vụ.
7. Repository chỉ xử lý data access.
8. Mapping dùng MapStruct hoặc mapper riêng.
9. Transaction đặt tại service.
10. Validate tổ hợp variant.
11. Không duplicate SKU.
12. Không duplicate variant signature.
13. Không cập nhật tồn kho mà không ghi transaction.
14. Dùng optimistic locking cho inventory.
15. Sinh Liquibase migration.
16. Không dùng `ddl-auto=create`.
17. Viết unit test và integration test.
18. API danh sách có pagination.
19. API dùng response wrapper chuẩn.

---

# 26. Test case bắt buộc

## Product

- Tạo SIMPLE product.
- Tạo VARIABLE product.
- Trùng code.
- Trùng slug.
- Brand không tồn tại.
- Category không tồn tại.
- Nhiều primary category.
- Nhiều primary image.

## Attribute

- Trùng code.
- Data type không hợp lệ.
- Attribute không cho variant nhưng được dùng tạo variant.
- Attribute value không thuộc attribute.

## Variant

- Sinh tổ hợp đúng.
- Trùng SKU.
- Trùng signature.
- Một variant có hai value cùng attribute.
- Giá âm.
- Sale price lớn hơn list price.
- Vượt giới hạn variant.
- Xóa variant đã phát sinh order.

## Inventory

- Nhập kho.
- Giữ hàng.
- Giải phóng.
- Không cho tồn âm.
- Không cho reserved vượt on-hand.
- Hai request cập nhật đồng thời.

---

# 27. Definition of Done

- Có Liquibase migration.
- Có entity và enum.
- Có repository.
- Có service và controller.
- Có DTO và mapper.
- Có validation.
- Có variant generator.
- Có inventory integration.
- Có image integration.
- Có API documentation.
- Có unit test.
- Có integration test.
- Không duplicate SKU.
- Không duplicate variant.
- Không tồn kho âm.
- Không dùng `double` cho tiền.
- Code compile thành công.
- Test chạy thành công.
- Không dùng entity làm response.
- Không tự động drop dữ liệu.

---

# 28. Kết luận

Thiết kế product động:

```text
Product
→ thông tin chung

Attribute
→ thuộc tính động

Attribute Value
→ giá trị chọn sẵn

Product Attribute Config
→ attribute áp dụng cho product

Product Attribute Value
→ thông số chung của product

Variant
→ SKU, giá, kích thước, trạng thái

Variant Attribute Value
→ tổ hợp thuộc tính của variant

Inventory
→ tồn kho theo variant và warehouse
```

Thiết kế này hỗ trợ quần áo, điện thoại, laptop, mỹ phẩm, nội thất và nhiều loại sản phẩm khác mà không phải thêm cột mới vào bảng `products`.
