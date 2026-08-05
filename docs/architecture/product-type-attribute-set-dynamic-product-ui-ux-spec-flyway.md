# Thiết kế Product Type, Attribute Set và UX/UI quản lý sản phẩm động

## 1. Mục tiêu

Tài liệu này mô tả thiết kế đầy đủ cho hệ thống thương mại điện tử bán nhiều loại mặt hàng, trong đó mỗi loại sản phẩm có tập thuộc tính khác nhau.

Mục tiêu chính:

- Không bắt người dùng chọn thủ công thuộc tính cho từng sản phẩm.
- Người dùng chỉ cần chọn `Product Type`.
- Hệ thống tự xác định `Attribute Set`.
- Frontend tự sinh form thuộc tính theo cấu hình.
- Hỗ trợ sản phẩm đơn giản và sản phẩm có biến thể.
- Hỗ trợ thuộc tính mô tả và thuộc tính tạo biến thể.
- Hỗ trợ phân nhóm thuộc tính trên giao diện.
- Có UX/UI quản lý loại sản phẩm, bộ thuộc tính, thuộc tính và giá trị thuộc tính.
- Có API và quy tắc để Claude Code sinh backend và frontend.

---

# 2. Vấn đề cần giải quyết

Hệ thống có thể bán rất nhiều nhóm mặt hàng:

```text
Điện thoại
Laptop
Áo thun
Giày dép
Tủ lạnh
Mỹ phẩm
Nội thất
Đồ gia dụng
```

Mỗi nhóm có các thuộc tính khác nhau.

Ví dụ:

```text
Điện thoại
- RAM
- Dung lượng
- Kích thước màn hình
- Dung lượng pin
- Camera
- Chip xử lý

Áo thun
- Màu sắc
- Kích thước
- Chất liệu
- Kiểu cổ
- Kiểu tay

Tủ lạnh
- Dung tích
- Số cửa
- Công nghệ inverter
- Công suất
- Điện năng tiêu thụ
```

Nếu khi tạo sản phẩm người dùng phải tìm thuộc tính trong một danh sách hàng trăm hoặc hàng nghìn thuộc tính thì UX sẽ rất kém và dễ nhập sai.

Giải pháp:

```text
Không chọn thuộc tính cho từng sản phẩm
→ Chọn loại sản phẩm
→ Loại sản phẩm liên kết với bộ thuộc tính
→ Hệ thống tự sinh form phù hợp
```

---

# 3. Phân biệt ba khái niệm

## 3.1. Category

Category phục vụ:

- Điều hướng.
- Tìm kiếm.
- Phân loại hiển thị.
- SEO.
- Marketing.
- Bộ lọc danh sách sản phẩm.

Ví dụ một sản phẩm có thể thuộc nhiều category:

```text
iPhone 16 Pro Max
├── Điện thoại
├── Điện thoại Apple
├── Sản phẩm mới
└── Sản phẩm cao cấp
```

Một sản phẩm có thể thuộc nhiều category.

## 3.2. Product Type

Product Type xác định bản chất kỹ thuật của sản phẩm.

Ví dụ:

```text
SMARTPHONE
LAPTOP
T_SHIRT
SHOES
REFRIGERATOR
COSMETICS
```

Một sản phẩm chỉ nên có một Product Type chính.

## 3.3. Attribute Set

Attribute Set là bộ thuộc tính mẫu áp dụng cho một Product Type.

Ví dụ:

```text
SMARTPHONE_DEFAULT
├── Màu sắc
├── Dung lượng
├── RAM
├── Màn hình
├── Chip xử lý
├── Camera
└── Dung lượng pin
```

Quan hệ:

```text
Product Type
→ Attribute Set
→ Attribute Groups
→ Attributes
```

---

# 4. Mô hình tổng thể

```text
product_types
    │
    └── default_attribute_set_id
            │
            ▼
attribute_sets
    │
    └── attribute_set_groups
    │       │
    │       └── attribute_set_attributes
    │               │
    │               ▼
    │         product_attributes
    │               │
    │               └── attribute_values
    │
    ▼
products
    ├── product_attribute_values
    ├── product_variants
    │       └── variant_attribute_values
    └── product_categories
```

Luồng tạo sản phẩm:

```text
Chọn Product Type
→ Lấy Attribute Set mặc định
→ Lấy Group và Attribute
→ Render form động
→ Nhập giá trị
→ Sinh variant
→ Lưu sản phẩm
```

---

# 5. Chuẩn UUID

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

`version` vẫn dùng `BIGINT` để phục vụ optimistic locking.

---

# 5.1. Chuẩn Flyway migration

Hệ thống sử dụng Flyway để quản lý database migration.

## Cấu trúc thư mục

Với Spring Boot:

```text
src/main/resources/db/migration/
├── V1__create_product_types.sql
├── V2__create_attribute_sets.sql
├── V3__create_attribute_set_groups.sql
├── V4__create_product_attributes.sql
├── V5__create_attribute_values.sql
├── V6__create_attribute_set_attributes.sql
├── V7__alter_products_add_product_type_and_attribute_set.sql
├── V8__create_product_attribute_values.sql
└── V9__create_product_attribute_overrides.sql
```

## Quy tắc đặt tên file

Định dạng:

```text
V<version>__<description>.sql
```

Ví dụ:

```text
V1__create_product_types.sql
V2__create_attribute_sets.sql
V3__create_attribute_set_groups.sql
```

Quy tắc:

- Dùng hai dấu gạch dưới `__` giữa version và description.
- Description dùng chữ thường và dấu gạch dưới.
- Không sửa nội dung migration đã chạy trên môi trường dùng chung.
- Khi cần thay đổi schema, tạo migration version mới.
- Không đổi tên file migration đã chạy.
- Không xóa migration đã chạy.
- Không dùng `ddl-auto=create`, `create-drop` hoặc `update`.
- Có thể dùng `validate` hoặc `none` cho Hibernate DDL.

## Thứ tự migration đề xuất

Do `product_types.default_attribute_set_id` tham chiếu đến `attribute_sets`, cần tránh circular dependency khi tạo bảng.

Thứ tự:

```text
V1  → create product_types chưa có foreign key default_attribute_set_id
V2  → create attribute_sets
V3  → add foreign key product_types.default_attribute_set_id
V4  → create attribute_set_groups
V5  → create product_attributes
V6  → create attribute_values
V7  → create attribute_set_attributes
V8  → alter products add product_type_id, attribute_set_id, attribute_set_version
V9  → create product_attribute_values
V10 → create product_attribute_overrides nếu triển khai
```

Có thể tạo column `default_attribute_set_id` ngay trong `V1`, nhưng chỉ thêm foreign key sau khi `attribute_sets` đã tồn tại.

## Cấu hình Spring Boot

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
    clean-disabled: true

  jpa:
    hibernate:
      ddl-auto: validate
```

Với database đã tồn tại trước khi tích hợp Flyway, chỉ bật `baseline-on-migrate` sau khi đánh giá kỹ trạng thái schema.

Không tự động bật:

```yaml
spring.flyway.clean-disabled: false
```

trên môi trường production.

## Transaction và khả năng chạy lại

- Migration phải chạy đúng một lần theo version.
- Không viết migration phụ thuộc vào dữ liệu tạm thời không kiểm soát.
- Với dữ liệu seed cố định, có thể dùng migration versioned.
- Không dùng câu lệnh phá hủy dữ liệu nếu chưa có phương án migrate dữ liệu.
- Mọi thay đổi column phải cân nhắc dữ liệu đang tồn tại.
- Khi thêm column bắt buộc, nên:
  1. thêm column nullable,
  2. migrate dữ liệu,
  3. thêm constraint `NOT NULL` trong migration sau.

## Seed dữ liệu

Có thể tạo:

```text
V11__seed_default_product_types.sql
V12__seed_default_attributes.sql
V13__seed_default_attribute_sets.sql
```

Seed phải:

- Có UUID cố định nếu cần tham chiếu chéo.
- Không sinh UUID ngẫu nhiên bằng application trong file SQL.
- Không insert trùng dữ liệu khi triển khai môi trường mới.
- Dùng unique code làm cơ sở kiểm tra.

## Chỉ dẫn cho Claude Code

Claude Code phải:

1. Kiểm tra migration version cao nhất hiện có.
2. Không ghi đè migration cũ.
3. Tạo version mới nối tiếp.
4. Tách migration theo nhóm thay đổi hợp lý.
5. Đảm bảo thứ tự foreign key hợp lệ.
6. Không gọi `flyway clean`.
7. Không bật Hibernate tự tạo bảng.
8. Báo rõ danh sách file migration được tạo.
9. Chạy test khởi động application để Flyway validate migration.
10. Nếu database hiện tại đã có bảng tương ứng, phải tạo migration theo hướng alter an toàn, không tạo trùng bảng.

---

# 6. Bảng `product_types`

## 6.1. Mục đích

Lưu loại sản phẩm.

Ví dụ:

```text
SMARTPHONE
LAPTOP
T_SHIRT
SHOES
REFRIGERATOR
COSMETICS
```

## 6.2. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `code` | VARCHAR(100) | Có | Mã loại sản phẩm |
| `name` | VARCHAR(255) | Có | Tên loại sản phẩm |
| `description` | VARCHAR(2000) | Không | Mô tả |
| `default_attribute_set_id` | UUID | Không | Bộ thuộc tính mặc định |
| `icon_url` | VARCHAR(1000) | Không | Icon |
| `status` | VARCHAR(30) | Có | ACTIVE, INACTIVE |
| `sort_order` | INT | Có | Thứ tự |
| `created_by` | UUID | Không | Người tạo |
| `updated_by` | UUID | Không | Người cập nhật |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |
| `deleted_at` | DATETIME | Không | Xóa mềm |

## 6.3. SQL

```sql
CREATE TABLE product_types (
    id CHAR(36) PRIMARY KEY,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NULL,

    default_attribute_set_id CHAR(36) NULL,
    icon_url VARCHAR(1000) NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NOT NULL DEFAULT 0,

    created_by CHAR(36) NULL,
    updated_by CHAR(36) NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    CONSTRAINT uk_product_types_code UNIQUE (code)
);
```

Khóa ngoại đến `attribute_sets` nên được tạo sau khi bảng `attribute_sets` tồn tại:

```sql
ALTER TABLE product_types
ADD CONSTRAINT fk_product_types_default_attribute_set
FOREIGN KEY (default_attribute_set_id)
REFERENCES attribute_sets(id);
```

## 6.4. Quy tắc

- `code` phải duy nhất.
- Product Type đang được product sử dụng không được xóa cứng.
- Product Type `INACTIVE` không được chọn khi tạo sản phẩm mới.
- Một Product Type có một Attribute Set mặc định.
- Có thể thay Attribute Set mặc định nhưng không tự động thay schema của sản phẩm cũ.

---

# 7. Bảng `attribute_sets`

## 7.1. Mục đích

Lưu bộ thuộc tính mẫu.

Ví dụ:

```text
SMARTPHONE_DEFAULT
LAPTOP_DEFAULT
T_SHIRT_DEFAULT
SHOES_DEFAULT
REFRIGERATOR_DEFAULT
```

## 7.2. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `code` | VARCHAR(100) | Có | Mã bộ thuộc tính |
| `name` | VARCHAR(255) | Có | Tên |
| `description` | VARCHAR(2000) | Không | Mô tả |
| `status` | VARCHAR(30) | Có | ACTIVE, INACTIVE |
| `version_no` | INT | Có | Phiên bản schema |
| `is_system` | BOOLEAN | Có | Bộ hệ thống |
| `created_by` | UUID | Không | Người tạo |
| `updated_by` | UUID | Không | Người cập nhật |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |
| `deleted_at` | DATETIME | Không | Xóa mềm |

## 7.3. SQL

```sql
CREATE TABLE attribute_sets (
    id CHAR(36) PRIMARY KEY,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    version_no INT NOT NULL DEFAULT 1,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,

    created_by CHAR(36) NULL,
    updated_by CHAR(36) NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    CONSTRAINT uk_attribute_sets_code UNIQUE (code)
);
```

## 7.4. Quy tắc

- Không xóa cứng Attribute Set đã được sử dụng.
- Khi thay đổi cấu trúc lớn, có thể tăng `version_no`.
- Attribute Set `INACTIVE` không được gán mới.
- Có chức năng clone Attribute Set.

---

# 8. Bảng `attribute_set_groups`

## 8.1. Mục đích

Phân nhóm thuộc tính trong một Attribute Set để render UI dễ hiểu.

Ví dụ bộ Smartphone:

```text
Thông tin chung
Màn hình
Phần cứng
Camera
Pin và sạc
Kết nối
Biến thể
```

## 8.2. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `attribute_set_id` | UUID | Có | Bộ thuộc tính |
| `code` | VARCHAR(100) | Có | Mã nhóm |
| `name` | VARCHAR(255) | Có | Tên nhóm |
| `description` | VARCHAR(1000) | Không | Mô tả |
| `is_collapsible` | BOOLEAN | Có | Cho phép thu gọn |
| `is_default_expanded` | BOOLEAN | Có | Mở sẵn |
| `sort_order` | INT | Có | Thứ tự |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |

## 8.3. SQL

```sql
CREATE TABLE attribute_set_groups (
    id CHAR(36) PRIMARY KEY,

    attribute_set_id CHAR(36) NOT NULL,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,

    is_collapsible BOOLEAN NOT NULL DEFAULT TRUE,
    is_default_expanded BOOLEAN NOT NULL DEFAULT TRUE,

    sort_order INT NOT NULL DEFAULT 0,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_attribute_set_groups_set
        FOREIGN KEY (attribute_set_id)
        REFERENCES attribute_sets(id),

    CONSTRAINT uk_attribute_set_groups
        UNIQUE (attribute_set_id, code)
);
```

---

# 9. Bảng `product_attributes`

## 9.1. Mục đích

Lưu định nghĩa thuộc tính dùng chung toàn hệ thống.

Ví dụ:

```text
COLOR
SIZE
RAM
STORAGE
SCREEN_SIZE
MATERIAL
BATTERY_CAPACITY
```

## 9.2. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `code` | VARCHAR(100) | Có | Mã thuộc tính |
| `name` | VARCHAR(255) | Có | Tên hiển thị |
| `description` | VARCHAR(1000) | Không | Mô tả |
| `data_type` | VARCHAR(30) | Có | Kiểu dữ liệu |
| `input_type` | VARCHAR(30) | Có | Kiểu component |
| `unit` | VARCHAR(50) | Không | Đơn vị |
| `allow_predefined_values` | BOOLEAN | Có | Có danh sách giá trị sẵn |
| `allow_custom_value` | BOOLEAN | Có | Cho nhập tự do |
| `allow_variant` | BOOLEAN | Có | Có thể tạo variant |
| `validation_rules` | LONGTEXT | Không | JSON validation |
| `status` | VARCHAR(30) | Có | ACTIVE, INACTIVE |
| `is_system` | BOOLEAN | Có | Thuộc tính hệ thống |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |
| `deleted_at` | DATETIME | Không | Xóa mềm |

## 9.3. SQL

```sql
CREATE TABLE product_attributes (
    id CHAR(36) PRIMARY KEY,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,

    data_type VARCHAR(30) NOT NULL,
    input_type VARCHAR(30) NOT NULL,
    unit VARCHAR(50) NULL,

    allow_predefined_values BOOLEAN NOT NULL DEFAULT FALSE,
    allow_custom_value BOOLEAN NOT NULL DEFAULT TRUE,
    allow_variant BOOLEAN NOT NULL DEFAULT FALSE,

    validation_rules LONGTEXT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    is_system BOOLEAN NOT NULL DEFAULT FALSE,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    CONSTRAINT uk_product_attributes_code UNIQUE (code)
);
```

## 9.4. Enum

```java
public enum AttributeDataType {
    STRING,
    INTEGER,
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
    SWITCH,
    COLOR,
    DATE,
    DATETIME
}
```

---

# 10. Bảng `attribute_values`

## 10.1. Mục đích

Lưu giá trị chọn sẵn cho thuộc tính.

Ví dụ:

```text
COLOR
- BLACK
- WHITE
- RED

SIZE
- S
- M
- L
- XL

STORAGE
- 128GB
- 256GB
- 512GB
```

## 10.2. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `attribute_id` | UUID | Có | Thuộc tính |
| `code` | VARCHAR(100) | Có | Mã giá trị |
| `value` | VARCHAR(500) | Có | Giá trị lưu |
| `display_value` | VARCHAR(500) | Không | Giá trị hiển thị |
| `color_code` | VARCHAR(20) | Không | Mã màu |
| `image_url` | VARCHAR(1000) | Không | Ảnh |
| `metadata` | LONGTEXT | Không | JSON mở rộng |
| `status` | VARCHAR(30) | Có | Trạng thái |
| `sort_order` | INT | Có | Thứ tự |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |

## 10.3. SQL

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

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NOT NULL DEFAULT 0,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_attribute_values_attribute
        FOREIGN KEY (attribute_id)
        REFERENCES product_attributes(id),

    CONSTRAINT uk_attribute_values
        UNIQUE (attribute_id, code)
);
```

---

# 11. Bảng `attribute_set_attributes`

## 11.1. Mục đích

Xác định một Attribute Set có những Attribute nào và cách Attribute đó hoạt động trong set.

Đây là bảng quan trọng nhất của thiết kế.

## 11.2. Field

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `attribute_set_id` | UUID | Có | Bộ thuộc tính |
| `group_id` | UUID | Không | Nhóm hiển thị |
| `attribute_id` | UUID | Có | Thuộc tính |
| `label_override` | VARCHAR(255) | Không | Tên hiển thị riêng |
| `help_text` | VARCHAR(1000) | Không | Hướng dẫn nhập |
| `placeholder` | VARCHAR(500) | Không | Placeholder |
| `is_required` | BOOLEAN | Có | Bắt buộc |
| `is_variant_attribute` | BOOLEAN | Có | Dùng tạo variant |
| `is_filterable` | BOOLEAN | Có | Dùng lọc |
| `is_searchable` | BOOLEAN | Có | Dùng tìm kiếm |
| `is_comparable` | BOOLEAN | Có | Dùng so sánh |
| `is_visible` | BOOLEAN | Có | Hiển thị cho khách |
| `is_editable` | BOOLEAN | Có | Cho sửa |
| `default_value_id` | UUID | Không | Giá trị mặc định |
| `default_value_text` | VARCHAR(2000) | Không | Giá trị mặc định tự do |
| `validation_override` | LONGTEXT | Không | JSON validation riêng |
| `sort_order` | INT | Có | Thứ tự |
| `created_at` | DATETIME | Có | Ngày tạo |
| `updated_at` | DATETIME | Có | Ngày cập nhật |

## 11.3. SQL

```sql
CREATE TABLE attribute_set_attributes (
    id CHAR(36) PRIMARY KEY,

    attribute_set_id CHAR(36) NOT NULL,
    group_id CHAR(36) NULL,
    attribute_id CHAR(36) NOT NULL,

    label_override VARCHAR(255) NULL,
    help_text VARCHAR(1000) NULL,
    placeholder VARCHAR(500) NULL,

    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_variant_attribute BOOLEAN NOT NULL DEFAULT FALSE,

    is_filterable BOOLEAN NOT NULL DEFAULT FALSE,
    is_searchable BOOLEAN NOT NULL DEFAULT FALSE,
    is_comparable BOOLEAN NOT NULL DEFAULT FALSE,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    is_editable BOOLEAN NOT NULL DEFAULT TRUE,

    default_value_id CHAR(36) NULL,
    default_value_text VARCHAR(2000) NULL,

    validation_override LONGTEXT NULL,

    sort_order INT NOT NULL DEFAULT 0,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_attribute_set_attributes_set
        FOREIGN KEY (attribute_set_id)
        REFERENCES attribute_sets(id),

    CONSTRAINT fk_attribute_set_attributes_group
        FOREIGN KEY (group_id)
        REFERENCES attribute_set_groups(id),

    CONSTRAINT fk_attribute_set_attributes_attribute
        FOREIGN KEY (attribute_id)
        REFERENCES product_attributes(id),

    CONSTRAINT fk_attribute_set_attributes_default_value
        FOREIGN KEY (default_value_id)
        REFERENCES attribute_values(id),

    CONSTRAINT uk_attribute_set_attributes
        UNIQUE (attribute_set_id, attribute_id)
);
```

## 11.4. Quy tắc

- Không gán trùng một Attribute vào cùng Attribute Set.
- `is_variant_attribute = true` chỉ hợp lệ khi `product_attributes.allow_variant = true`.
- `default_value_id` phải thuộc đúng Attribute.
- Thuộc tính `INACTIVE` không được thêm mới.
- Có thể kéo thả để thay đổi `sort_order`.
- Group phải thuộc cùng Attribute Set.

---

# 12. Sửa bảng `products`

Bổ sung:

```sql
ALTER TABLE products
ADD COLUMN product_type_id CHAR(36) NULL;

ALTER TABLE products
ADD COLUMN attribute_set_id CHAR(36) NULL;

ALTER TABLE products
ADD COLUMN attribute_set_version INT NULL;

ALTER TABLE products
ADD CONSTRAINT fk_products_product_type
FOREIGN KEY (product_type_id)
REFERENCES product_types(id);

ALTER TABLE products
ADD CONSTRAINT fk_products_attribute_set
FOREIGN KEY (attribute_set_id)
REFERENCES attribute_sets(id);
```

## Ý nghĩa

| Field | Ý nghĩa |
|---|---|
| `product_type_id` | Loại sản phẩm |
| `attribute_set_id` | Bộ thuộc tính áp dụng |
| `attribute_set_version` | Phiên bản schema tại thời điểm tạo |

Khi tạo sản phẩm:

```text
product_type_id
→ lấy default_attribute_set_id
→ lưu vào attribute_set_id
→ lưu version_no vào attribute_set_version
```

Điều này giúp sản phẩm cũ không bị thay đổi ngầm khi cấu hình Attribute Set được sửa sau này.

---

# 13. Bảng `product_attribute_values`

## 13.1. Mục đích

Lưu giá trị thuộc tính mô tả của sản phẩm.

Ví dụ:

```text
RAM = 8GB
SCREEN_SIZE = 6.9 inch
CHIPSET = A18 Pro
MATERIAL = Cotton
```

## 13.2. SQL

```sql
CREATE TABLE product_attribute_values (
    id CHAR(36) PRIMARY KEY,

    product_id CHAR(36) NOT NULL,
    attribute_id CHAR(36) NOT NULL,
    attribute_value_id CHAR(36) NULL,

    value_text VARCHAR(2000) NULL,
    value_integer BIGINT NULL,
    value_decimal DECIMAL(19, 4) NULL,
    value_boolean BOOLEAN NULL,
    value_date DATE NULL,
    value_datetime DATETIME(6) NULL,
    value_json LONGTEXT NULL,

    display_value VARCHAR(2000) NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_product_attribute_values_product
        FOREIGN KEY (product_id)
        REFERENCES products(id),

    CONSTRAINT fk_product_attribute_values_attribute
        FOREIGN KEY (attribute_id)
        REFERENCES product_attributes(id),

    CONSTRAINT fk_product_attribute_values_predefined
        FOREIGN KEY (attribute_value_id)
        REFERENCES attribute_values(id)
);
```

## 13.3. Quy tắc

- Chỉ dùng một field value phù hợp với `data_type`.
- Thuộc tính phải nằm trong Attribute Set của Product.
- Không cho nhập Attribute ngoài schema, trừ khi dùng cơ chế override.
- Nếu thuộc tính dùng `SELECT`, ưu tiên `attribute_value_id`.
- Nếu cho phép custom value, có thể lưu `value_text`.

---

# 14. Bảng override tùy chọn

Chỉ dùng khi một Product đặc biệt cần thêm hoặc bỏ Attribute ngoài Attribute Set.

## 14.1. Bảng `product_attribute_overrides`

```sql
CREATE TABLE product_attribute_overrides (
    id CHAR(36) PRIMARY KEY,

    product_id CHAR(36) NOT NULL,
    attribute_id CHAR(36) NOT NULL,

    override_type VARCHAR(30) NOT NULL,

    is_required BOOLEAN NULL,
    is_variant_attribute BOOLEAN NULL,
    is_visible BOOLEAN NULL,

    sort_order INT NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_product_attribute_overrides_product
        FOREIGN KEY (product_id)
        REFERENCES products(id),

    CONSTRAINT fk_product_attribute_overrides_attribute
        FOREIGN KEY (attribute_id)
        REFERENCES product_attributes(id),

    CONSTRAINT uk_product_attribute_overrides
        UNIQUE (product_id, attribute_id)
);
```

Enum:

```java
public enum ProductAttributeOverrideType {
    ADD,
    REMOVE,
    OVERRIDE
}
```

Khuyến nghị:

- Không triển khai override trong MVP nếu chưa thực sự cần.
- Không để override trở thành cách cấu hình chính.
- Attribute Set vẫn là nguồn cấu hình mặc định.

---

# 15. API lấy schema form động

```http
GET /api/v1/admin/product-types/{productTypeId}/attribute-schema
```

## Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "data": {
    "productType": {
      "id": "product-type-uuid",
      "code": "SMARTPHONE",
      "name": "Điện thoại thông minh"
    },
    "attributeSet": {
      "id": "attribute-set-uuid",
      "code": "SMARTPHONE_DEFAULT",
      "name": "Bộ thuộc tính Smartphone",
      "version": 3
    },
    "groups": [
      {
        "id": "group-display-uuid",
        "code": "DISPLAY",
        "name": "Màn hình",
        "sortOrder": 1,
        "collapsible": true,
        "defaultExpanded": true,
        "attributes": [
          {
            "id": "screen-size-uuid",
            "code": "SCREEN_SIZE",
            "name": "Kích thước màn hình",
            "label": "Kích thước màn hình",
            "dataType": "DECIMAL",
            "inputType": "NUMBER",
            "unit": "inch",
            "required": true,
            "variantAttribute": false,
            "filterable": true,
            "searchable": false,
            "comparable": true,
            "visible": true,
            "editable": true,
            "placeholder": "Ví dụ: 6.9",
            "helpText": "Nhập kích thước đường chéo màn hình",
            "validation": {
              "min": 1,
              "max": 100,
              "decimalScale": 2
            }
          }
        ]
      },
      {
        "id": "group-variant-uuid",
        "code": "VARIANT",
        "name": "Biến thể",
        "sortOrder": 99,
        "attributes": [
          {
            "id": "color-uuid",
            "code": "COLOR",
            "name": "Màu sắc",
            "dataType": "STRING",
            "inputType": "COLOR",
            "required": true,
            "variantAttribute": true,
            "values": [
              {
                "id": "black-uuid",
                "code": "BLACK",
                "value": "BLACK",
                "displayValue": "Đen",
                "colorCode": "#000000"
              },
              {
                "id": "white-uuid",
                "code": "WHITE",
                "value": "WHITE",
                "displayValue": "Trắng",
                "colorCode": "#FFFFFF"
              }
            ]
          }
        ]
      }
    ]
  }
}
```

---

# 16. API quản lý Product Type

```text
GET    /api/v1/admin/product-types
GET    /api/v1/admin/product-types/{id}
POST   /api/v1/admin/product-types
PUT    /api/v1/admin/product-types/{id}
DELETE /api/v1/admin/product-types/{id}
POST   /api/v1/admin/product-types/{id}/activate
POST   /api/v1/admin/product-types/{id}/deactivate
```

Request tạo:

```json
{
  "code": "SMARTPHONE",
  "name": "Điện thoại thông minh",
  "description": "Sản phẩm điện thoại thông minh",
  "defaultAttributeSetId": "attribute-set-uuid",
  "status": "ACTIVE",
  "sortOrder": 1
}
```

---

# 17. API quản lý Attribute Set

```text
GET    /api/v1/admin/attribute-sets
GET    /api/v1/admin/attribute-sets/{id}
POST   /api/v1/admin/attribute-sets
PUT    /api/v1/admin/attribute-sets/{id}
DELETE /api/v1/admin/attribute-sets/{id}
POST   /api/v1/admin/attribute-sets/{id}/clone
PUT    /api/v1/admin/attribute-sets/{id}/schema
```

Request cập nhật schema:

```json
{
  "groups": [
    {
      "id": "group-display-uuid",
      "code": "DISPLAY",
      "name": "Màn hình",
      "sortOrder": 1,
      "attributes": [
        {
          "attributeId": "screen-size-uuid",
          "isRequired": true,
          "isVariantAttribute": false,
          "isFilterable": true,
          "isSearchable": false,
          "isComparable": true,
          "isVisible": true,
          "isEditable": true,
          "sortOrder": 1
        }
      ]
    }
  ]
}
```

Backend nên cập nhật toàn bộ schema trong một transaction.

---

# 18. API quản lý Attribute

```text
GET    /api/v1/admin/product-attributes
GET    /api/v1/admin/product-attributes/{id}
POST   /api/v1/admin/product-attributes
PUT    /api/v1/admin/product-attributes/{id}
DELETE /api/v1/admin/product-attributes/{id}

GET    /api/v1/admin/product-attributes/{id}/values
POST   /api/v1/admin/product-attributes/{id}/values
PUT    /api/v1/admin/product-attributes/{id}/values/{valueId}
DELETE /api/v1/admin/product-attributes/{id}/values/{valueId}
```

---

# 19. UX/UI tổng thể

Menu đề xuất:

```text
Quản lý sản phẩm
├── Sản phẩm
├── Danh mục
├── Thương hiệu
├── Loại sản phẩm
├── Bộ thuộc tính
├── Thuộc tính
└── Giá trị thuộc tính
```

Vai trò:

```text
Sản phẩm
→ nhập và quản lý dữ liệu sản phẩm

Loại sản phẩm
→ chọn bộ thuộc tính mặc định

Bộ thuộc tính
→ cấu hình form động

Thuộc tính
→ quản lý định nghĩa field dùng chung

Giá trị thuộc tính
→ quản lý option của select, color, radio
```

---

# 20. Màn hình danh sách Product Type

Route:

```text
/admin/product-types
```

Header:

```text
Quản lý loại sản phẩm                         [+ Thêm loại sản phẩm]
```

Filter:

```text
[Tìm theo mã hoặc tên] [Trạng thái ▼] [Bộ thuộc tính ▼] [Tìm kiếm]
```

Bảng:

| Cột | Nội dung |
|---|---|
| Mã | SMARTPHONE |
| Tên | Điện thoại thông minh |
| Bộ thuộc tính mặc định | SMARTPHONE_DEFAULT |
| Số sản phẩm | 1.250 |
| Trạng thái | Hoạt động |
| Cập nhật gần nhất | Thời gian |
| Thao tác | Xem, sửa, khóa |

Hành vi:

- Click tên để mở chi tiết.
- Không cho xóa nếu đang có product.
- Khi khóa, hiển thị số product bị ảnh hưởng.
- Product cũ vẫn hoạt động, nhưng không tạo mới bằng Product Type bị khóa.

---

# 21. Form tạo Product Type

Route:

```text
/admin/product-types/create
/admin/product-types/{id}/edit
```

Layout:

```text
┌──────────────────────────────────────────────┐
│ Thông tin loại sản phẩm                     │
├──────────────────────────────────────────────┤
│ Mã loại sản phẩm *                          │
│ [SMARTPHONE______________________________]  │
│                                              │
│ Tên loại sản phẩm *                         │
│ [Điện thoại thông minh__________________]   │
│                                              │
│ Bộ thuộc tính mặc định *                    │
│ [Bộ thuộc tính Smartphone______________▼]  │
│                                              │
│ Mô tả                                       │
│ [........................................]  │
│                                              │
│ Trạng thái                                  │
│ (●) Hoạt động   ( ) Ngừng hoạt động         │
│                                              │
│                              [Hủy] [Lưu]     │
└──────────────────────────────────────────────┘
```

Validation:

- Code viết hoa, không dấu, dùng `_`.
- Name bắt buộc.
- Attribute Set bắt buộc nếu Product Type ACTIVE.
- Code duy nhất.

---

# 22. Màn hình danh sách Attribute Set

Route:

```text
/admin/attribute-sets
```

Bảng:

| Cột | Nội dung |
|---|---|
| Mã | SMARTPHONE_DEFAULT |
| Tên | Bộ thuộc tính Smartphone |
| Số nhóm | 6 |
| Số thuộc tính | 28 |
| Số loại sản phẩm đang dùng | 2 |
| Phiên bản | 3 |
| Trạng thái | Active |
| Thao tác | Xem, sửa, clone |

Có nút:

```text
[+ Tạo bộ thuộc tính]
```

Có chức năng clone:

```text
SMARTPHONE_DEFAULT
→ Clone thành TABLET_DEFAULT
```

---

# 23. UX/UI cấu hình Attribute Set

Route:

```text
/admin/attribute-sets/{id}/builder
```

Layout ba cột:

```text
┌────────────────────┬───────────────────────────────┬───────────────────────┐
│ Thư viện thuộc tính│ Cấu trúc bộ thuộc tính      │ Cấu hình thuộc tính  │
├────────────────────┼───────────────────────────────┼───────────────────────┤
│ [Tìm thuộc tính]   │ Nhóm: Màn hình              │ Tên hiển thị         │
│                    │  ├ Kích thước màn hình      │ [................]   │
│ ☐ RAM              │  └ Loại màn hình            │                      │
│ ☐ Dung lượng       │                              │ Bắt buộc      [✓]   │
│ ☐ Màu sắc          │ Nhóm: Phần cứng             │ Tạo variant   [ ]   │
│ ☐ Chất liệu        │  ├ Chip xử lý                │ Dùng lọc      [✓]   │
│ ☐ Dung tích        │  ├ RAM                       │ Hiển thị      [✓]   │
│                    │  └ Dung lượng                │                      │
│ [+ Tạo thuộc tính] │                              │ Placeholder          │
│                    │ [+ Thêm nhóm]                │ [................]   │
└────────────────────┴───────────────────────────────┴───────────────────────┘
```

Cột trái:

- Search.
- Filter theo data type.
- Filter theo trạng thái.
- Filter `allow_variant`.
- Checkbox hoặc drag-and-drop.
- Nút tạo nhanh Attribute.

Thuộc tính đã nằm trong set phải disable hoặc đánh dấu:

```text
RAM                         Đã thêm
Màu sắc                     Đã thêm
Dung tích                   Chưa thêm
```

Cột giữa:

- Hiển thị group.
- Kéo thả group.
- Kéo thả Attribute trong group.
- Chuyển Attribute giữa group.
- Thu gọn/mở rộng group.
- Thêm, sửa, xóa group.
- Hiển thị badge:
  - Required.
  - Variant.
  - Filterable.
  - Hidden.

Cột phải:

```text
Tên hiển thị
Hướng dẫn nhập
Placeholder
Bắt buộc
Dùng tạo biến thể
Dùng bộ lọc
Dùng tìm kiếm
Dùng so sánh
Hiển thị cho khách
Cho phép chỉnh sửa
Giá trị mặc định
Validation
```

Sticky footer:

```text
[Hủy thay đổi] [Xem trước form] [Lưu nháp] [Lưu và áp dụng]
```

Không gọi API mỗi lần kéo thả.

---

# 24. Preview form

Khi nhấn `Xem trước form`, mở drawer hoặc modal:

```text
Xem trước form sản phẩm Smartphone

Thông tin màn hình
- Kích thước màn hình [________] inch
- Loại màn hình      [________▼]

Phần cứng
- Chip xử lý         [________]
- RAM                 [________▼]
- Dung lượng          [________▼]

Biến thể
- Màu sắc             [☐ Đen] [☐ Trắng]
- Dung lượng          [☐ 128GB] [☐ 256GB]
```

Preview phải dùng đúng component frontend thực tế.

---

# 25. Màn hình danh sách Attribute

Route:

```text
/admin/product-attributes
```

Filter:

```text
[Tìm mã/tên] [Data type ▼] [Input type ▼] [Cho tạo variant ▼] [Trạng thái ▼]
```

Bảng:

| Cột | Nội dung |
|---|---|
| Mã | SCREEN_SIZE |
| Tên | Kích thước màn hình |
| Data type | DECIMAL |
| Input type | NUMBER |
| Đơn vị | inch |
| Cho variant | Không |
| Số Attribute Set sử dụng | 4 |
| Trạng thái | Active |
| Thao tác | Xem, sửa, khóa |

---

# 26. Form Attribute

```text
Mã thuộc tính *
Tên thuộc tính *
Mô tả
Data type *
Input type *
Đơn vị
Có giá trị định nghĩa sẵn
Cho phép nhập giá trị tùy chọn
Có thể dùng tạo variant
Validation
Trạng thái
```

Dynamic UI:

- `input_type = SELECT` thì hiển thị khu vực quản lý option.
- `input_type = COLOR` thì option có color picker.
- `data_type = NUMBER/DECIMAL` thì hiển thị min, max, step.
- `data_type = STRING` thì hiển thị minLength, maxLength, regex.
- `input_type = MULTI_SELECT` không nên dùng làm variant trong MVP.

---

# 27. Quản lý Attribute Value

Có thể đặt trong trang chi tiết Attribute.

```text
Thuộc tính: Màu sắc

[+ Thêm giá trị]

Mã       Tên hiển thị   Màu      Thứ tự   Trạng thái
BLACK    Đen            #000000  1        Active
WHITE    Trắng          #FFFFFF  2        Active
RED      Đỏ             #FF0000  3        Active
```

Có:

- Kéo thả sắp xếp.
- Thêm nhanh nhiều giá trị.
- Import CSV nếu số lượng lớn.
- Không xóa cứng value đang được sử dụng.
- Chỉ cho chuyển `INACTIVE`.

---

# 28. UX/UI tạo sản phẩm

Route:

```text
/admin/products/create
```

Nên dùng wizard hoặc stepper:

```text
1. Thông tin chung
2. Phân loại
3. Thuộc tính
4. Biến thể
5. Hình ảnh và tồn kho
6. SEO và xuất bản
```

---

# 29. Bước 1: Thông tin chung

Field:

```text
Mã sản phẩm
Tên sản phẩm
Mô tả ngắn
Mô tả chi tiết
Thương hiệu
Ảnh đại diện
```

Không hiển thị thuộc tính tại bước này.

---

# 30. Bước 2: Phân loại

Field:

```text
Loại sản phẩm *
Danh mục chính *
Danh mục phụ
```

Ví dụ:

```text
Loại sản phẩm:
[Điện thoại thông minh ▼]

Danh mục chính:
[Điện thoại Apple ▼]

Danh mục phụ:
[Điện thoại] [Sản phẩm cao cấp] [Sản phẩm mới]
```

Khi chọn Product Type:

```text
Frontend gọi API schema
→ hiển thị tên Attribute Set
→ thông báo số nhóm và thuộc tính
```

Ví dụ:

```text
Bộ thuộc tính áp dụng:
Bộ thuộc tính Smartphone - Phiên bản 3

6 nhóm · 28 thuộc tính · 2 thuộc tính tạo biến thể
```

---

# 31. Bước 3: Thuộc tính

Frontend render form theo group.

```text
Màn hình
├── Kích thước màn hình *
├── Loại màn hình
└── Độ phân giải

Phần cứng
├── Chip xử lý *
├── RAM *
└── Hệ điều hành

Camera
├── Camera trước
└── Camera sau
```

UX:

- Group có thể collapse.
- Hiển thị số field chưa hoàn thành.
- Field required có dấu `*`.
- Hiển thị help text.
- Có sticky navigation bên trái nếu form dài.
- Có nút `Chỉ hiển thị trường bắt buộc`.
- Có search field trong form nếu số lượng thuộc tính lớn.

Không cho người dùng chọn Attribute thủ công tại đây.

---

# 32. Bước 4: Biến thể

Frontend chỉ hiển thị Attribute có:

```text
is_variant_attribute = true
```

Ví dụ:

```text
Màu sắc:
[☑ Đen] [☑ Trắng] [ ] Đỏ

Dung lượng:
[☑ 128GB] [☑ 256GB] [ ] 512GB
```

Sau khi chọn:

```text
[ Sinh tổ hợp biến thể ]
```

Kết quả:

| Biến thể | SKU | Giá bán | Giá niêm yết | Trạng thái |
|---|---|---:|---:|---|
| Đen / 128GB | IP16-BL-128 | ... | ... | Active |
| Đen / 256GB | IP16-BL-256 | ... | ... | Active |
| Trắng / 128GB | IP16-WH-128 | ... | ... | Active |
| Trắng / 256GB | IP16-WH-256 | ... | ... | Active |

UX hỗ trợ:

- Bulk update giá.
- Bulk update khối lượng.
- Bulk prefix SKU.
- Copy dữ liệu từ variant trước.
- Xóa một tổ hợp.
- Regenerate có cảnh báo mất dữ liệu.
- Giới hạn số variant.

---

# 33. Bước 5: Hình ảnh và tồn kho

Mỗi variant có thể:

- Dùng ảnh chung của product.
- Hoặc có ảnh riêng.
- Có tồn kho theo warehouse.

Ví dụ:

```text
Variant: Đen / 128GB

Ảnh:
[Upload] [Dùng ảnh chung]

Kho Hà Nội: 100
Kho TP.HCM: 80
```

Có chế độ bulk:

```text
Áp dụng cùng tồn kho cho tất cả variant
Áp dụng cùng kích thước cho tất cả variant
```

---

# 34. Bước 6: SEO và xuất bản

Field:

```text
Slug
SEO title
SEO description
SEO keywords
Canonical URL
Sản phẩm nổi bật
Ngày xuất bản
Sản phẩm mới đến ngày
Trạng thái
```

Có preview:

```text
Google search preview
Product page preview
```

---

# 35. Khi đổi Product Type trong form

Nếu người dùng đã nhập thuộc tính hoặc tạo variant, cần cảnh báo:

```text
Bạn đang thay đổi loại sản phẩm.

Dữ liệu thuộc tính và biến thể hiện tại có thể không còn phù hợp.

Dữ liệu sẽ bị ảnh hưởng:
- 18 giá trị thuộc tính
- 4 biến thể

[Hủy] [Đổi loại sản phẩm]
```

Không đổi ngay bằng một click.

---

# 36. Khi sửa Attribute Set đang được sử dụng

Hiển thị phạm vi ảnh hưởng:

```text
Bộ thuộc tính này đang được sử dụng bởi:
- 2 loại sản phẩm
- 1.250 sản phẩm

Thay đổi chỉ áp dụng mặc định cho sản phẩm tạo mới.
Sản phẩm cũ vẫn giữ phiên bản schema hiện tại.
```

MVP nên hỗ trợ:

```text
Lưu phiên bản mới
→ sản phẩm mới sử dụng version mới
→ sản phẩm cũ giữ version cũ
```

---

# 37. UI trạng thái và cảnh báo

Attribute bị khóa:

```text
Thuộc tính đã ngừng hoạt động.
Không thể thêm vào Attribute Set mới.
```

Attribute Value đang được sử dụng:

```text
Giá trị này đang được sử dụng bởi 428 biến thể.
Bạn chỉ có thể ngừng hoạt động, không thể xóa.
```

Attribute Set đang dùng:

```text
Bộ thuộc tính đang được 2 loại sản phẩm sử dụng.
Không thể xóa.
```

Product Type đang dùng:

```text
Loại sản phẩm đang được 1.250 sản phẩm sử dụng.
Không thể xóa.
```

---

# 38. Frontend component đề xuất

```text
src/modules/product-config/
├── product-type/
│   ├── api/
│   ├── components/
│   ├── models/
│   └── views/
│
├── attribute-set/
│   ├── api/
│   ├── components/
│   │   ├── AttributeLibrary.vue
│   │   ├── AttributeSetBuilder.vue
│   │   ├── AttributeGroupCard.vue
│   │   ├── AttributeConfigPanel.vue
│   │   ├── AttributeSetPreview.vue
│   │   └── AttributeSetImpactDialog.vue
│   ├── composables/
│   ├── models/
│   └── views/
│
├── attribute/
│   ├── components/
│   │   ├── AttributeForm.vue
│   │   ├── AttributeValueTable.vue
│   │   └── ValidationRuleEditor.vue
│   └── views/
│
└── dynamic-form/
    ├── DynamicAttributeForm.vue
    ├── DynamicAttributeField.vue
    ├── AttributeGroupSection.vue
    ├── VariantAttributeSelector.vue
    └── VariantCombinationTable.vue
```

---

# 39. Dynamic component mapping

```typescript
const componentByInputType = {
  TEXT: TextInput,
  TEXTAREA: TextArea,
  NUMBER: NumberInput,
  SELECT: SelectInput,
  MULTI_SELECT: MultiSelectInput,
  RADIO: RadioGroup,
  CHECKBOX: CheckboxGroup,
  SWITCH: SwitchInput,
  COLOR: ColorSelector,
  DATE: DatePicker,
  DATETIME: DateTimePicker
}
```

Frontend không hard-code:

```text
RAM
COLOR
SIZE
SCREEN_SIZE
```

Frontend chỉ đọc schema và render theo:

```text
dataType
inputType
required
values
validation
```

---

# 40. Validation frontend và backend

Frontend sinh validation động từ schema.

Ví dụ:

```json
{
  "required": true,
  "min": 1,
  "max": 100,
  "decimalScale": 2
}
```

Backend vẫn phải validate lại toàn bộ.

Không tin dữ liệu frontend.

---

# 41. Luồng lưu Attribute Set Builder

Frontend gửi toàn bộ snapshot schema:

```text
Groups
→ Attributes
→ Config
→ Sort order
```

Backend:

```text
1. Validate Attribute Set.
2. Validate duplicate group code.
3. Validate duplicate attribute.
4. Validate group thuộc Attribute Set.
5. Validate variant permission.
6. Validate default value.
7. Ghi mapping mới.
8. Tăng version nếu cần.
9. Ghi audit log.
10. Commit transaction.
```

---

# 42. Quy tắc nghiệp vụ bắt buộc

1. Product Type quyết định Attribute Set mặc định.
2. Category không quyết định Attribute Set.
3. Một Product có một Product Type chính.
4. Một Product có thể có nhiều Category.
5. Attribute Set có nhiều Group.
6. Group có nhiều Attribute.
7. Attribute có thể tái sử dụng trong nhiều Attribute Set.
8. Không cho Attribute trùng trong cùng Set.
9. Không cho Group thuộc Set khác.
10. Chỉ Attribute `allow_variant = true` mới được đánh dấu variant.
11. Không cho Product nhập Attribute ngoài schema.
12. Product lưu `attribute_set_version`.
13. Product cũ không tự thay schema khi Set thay đổi.
14. Attribute Value đang dùng không được xóa cứng.
15. Product Type đang dùng không được xóa cứng.
16. Attribute Set đang dùng không được xóa cứng.
17. Backend phải kiểm tra schema khi tạo và sửa Product.
18. Frontend render form từ API schema.
19. Không hard-code field theo từng loại sản phẩm.
20. Mọi thay đổi cấu hình quan trọng phải ghi audit log.

---

# 43. Permission đề xuất

```text
PRODUCT_TYPE_VIEW
PRODUCT_TYPE_CREATE
PRODUCT_TYPE_UPDATE
PRODUCT_TYPE_DELETE

ATTRIBUTE_SET_VIEW
ATTRIBUTE_SET_CREATE
ATTRIBUTE_SET_UPDATE
ATTRIBUTE_SET_DELETE
ATTRIBUTE_SET_CLONE

PRODUCT_ATTRIBUTE_VIEW
PRODUCT_ATTRIBUTE_CREATE
PRODUCT_ATTRIBUTE_UPDATE
PRODUCT_ATTRIBUTE_DELETE

ATTRIBUTE_VALUE_VIEW
ATTRIBUTE_VALUE_CREATE
ATTRIBUTE_VALUE_UPDATE
ATTRIBUTE_VALUE_DELETE
```

---

# 44. Audit log

Cần audit:

- Tạo Product Type.
- Đổi Attribute Set mặc định.
- Tạo Attribute Set.
- Thêm hoặc xóa Attribute khỏi Set.
- Đổi Attribute thành variant.
- Thay validation.
- Khóa Attribute.
- Khóa Attribute Value.
- Thay đổi schema version.

Ví dụ:

```json
{
  "module": "ATTRIBUTE_SET",
  "action": "UPDATE_SCHEMA",
  "objectId": "attribute-set-uuid",
  "description": "Cập nhật bộ thuộc tính Smartphone",
  "oldValue": {
    "version": 2,
    "attributeCount": 27
  },
  "newValue": {
    "version": 3,
    "attributeCount": 28
  }
}
```

---

# 45. Package backend đề xuất

```text
com.example.ecommerce.catalog
├── producttype
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   ├── service
│   └── mapper
│
├── attributeset
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   ├── service
│   ├── mapper
│   └── validator
│
├── attribute
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   ├── service
│   └── mapper
│
├── dynamicform
│   ├── service
│   ├── schema
│   └── validator
│
└── product
    ├── controller
    ├── dto
    ├── entity
    ├── repository
    ├── service
    └── validator
```

---

# 46. Test case bắt buộc

## Product Type

- Tạo Product Type.
- Code trùng.
- Gán Attribute Set không tồn tại.
- Gán Attribute Set inactive.
- Không xóa Product Type đang dùng.
- Khóa Product Type.

## Attribute Set

- Tạo Set.
- Clone Set.
- Thêm Group.
- Trùng Group code.
- Thêm Attribute.
- Trùng Attribute.
- Attribute inactive.
- Attribute variant không hợp lệ.
- Default value không thuộc Attribute.
- Group không thuộc Set.
- Thay đổi sort order.
- Lưu version mới.

## Dynamic schema

- Lấy schema theo Product Type.
- Product Type không có default Set.
- Set inactive.
- Group order đúng.
- Attribute order đúng.
- Value order đúng.
- Validation trả đúng.

## Product

- Tạo Product theo schema.
- Thiếu Attribute required.
- Nhập Attribute ngoài schema.
- Sai data type.
- Sai predefined value.
- Tạo variant từ Attribute không cho variant.
- Đổi Product Type sau khi đã có dữ liệu.
- Product giữ Attribute Set version cũ.

## UX

- Search Attribute.
- Drag Attribute vào Group.
- Không kéo Attribute trùng.
- Preview form.
- Cảnh báo thay đổi chưa lưu.
- Cảnh báo phạm vi ảnh hưởng.
- Regenerate variant có xác nhận.

---

# 47. Definition of Done

Module hoàn thành khi:

- Có Flyway migration.
- Migration dùng version mới, không sửa file đã chạy.
- Flyway validate thành công.
- Hibernate `ddl-auto` đặt là `validate` hoặc `none`.
- Có entity, enum và repository.
- Có service và controller.
- Có API schema động.
- Có Attribute Set Builder.
- Có preview form.
- Có Product Type management.
- Có Attribute management.
- Có Attribute Value management.
- Có dynamic product form.
- Có variant attribute selector.
- Có validation frontend và backend.
- Có audit log.
- Có permission.
- Có unit test.
- Có integration test.
- Có frontend test cho component quan trọng.
- Không hard-code thuộc tính theo loại sản phẩm.
- Không cho nhập thuộc tính ngoài schema.
- Code compile thành công.
- Test chạy thành công.

---

# 48. Chỉ dẫn trực tiếp cho Claude Code

Hãy triển khai module theo tài liệu này.

Yêu cầu:

1. Kiểm tra cấu trúc source code hiện tại trước khi tạo file.
2. Tái sử dụng response wrapper, exception handler, audit base và base entity hiện có.
3. Dùng UUID cho toàn bộ ID.
4. Dùng Flyway migration theo chuẩn `V<version>__<description>.sql`; không sửa migration đã chạy và không dùng `ddl-auto=create`.
5. Không thay đổi hoặc xóa dữ liệu hiện có.
6. Backend phải sinh schema form động.
7. Frontend phải render theo schema, không hard-code Attribute.
8. Attribute Set Builder phải hỗ trợ:
   - group,
   - drag-and-drop,
   - search Attribute,
   - config Attribute,
   - preview,
   - lưu snapshot.
9. Tất cả API admin phải kiểm tra permission.
10. Viết test cho nghiệp vụ quan trọng.
11. Liệt kê file được tạo và file được sửa sau khi hoàn thành.
12. Không để TODO hoặc mock data trong code production.

Thứ tự triển khai:

```text
Phase 1
→ Kiểm tra version Flyway hiện tại
→ Tạo Flyway migration mới
→ Entity
→ Repository

Phase 2
→ Product Type API
→ Attribute API
→ Attribute Value API

Phase 3
→ Attribute Set API
→ Schema API
→ Validation

Phase 4
→ Frontend danh sách và form cơ bản

Phase 5
→ Attribute Set Builder
→ Preview form
→ Dynamic product form

Phase 6
→ Audit
→ Permission
→ Test
→ Documentation
```

---

# 49. Kết luận

Thiết kế cuối cùng:

```text
Category
→ phục vụ điều hướng và marketing

Product Type
→ xác định loại sản phẩm

Attribute Set
→ xác định form nhập dữ liệu

Attribute Group
→ phân nhóm UI

Attribute
→ định nghĩa field động

Attribute Value
→ option chọn sẵn

Product Attribute Value
→ dữ liệu thực tế của Product

Variant Attribute Value
→ dữ liệu tạo biến thể
```

Luồng UX tối ưu:

```text
Người dùng chọn Product Type
→ Hệ thống tự lấy Attribute Set
→ Frontend tự sinh form
→ Người dùng chỉ nhập dữ liệu
→ Không phải tìm thuộc tính thủ công
```
