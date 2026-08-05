# Thiết kế module User, Authentication, Authorization và Audit

> **Trạng thái trong dự án này:** đây là tài liệu thiết kế mục tiêu (aspirational), chưa triển khai toàn bộ. Khác biệt đã chốt so với bản gốc:
> - Dùng **Flyway** (không phải Liquibase) - khớp với toàn bộ `user-service`/`product-service` hiện có.
> - Đăng nhập vẫn dùng **email**, không đổi sang `username` (cột `username` trong mục 5 không triển khai).
> - Triển khai theo từng phase, mở rộng schema trước rồi mới tới các bảng/luồng mới (`customer_addresses`, `password_reset_tokens`, `audit_logs`, `login_histories`, Redis session nâng cao...). Tiến độ thực tế: xem `V5__expand_user_rbac_schema.sql` trở đi trong `backend/user-service/src/main/resources/db/migration/` và `docs/LOCAL_DEV_SETUP.md`.

## 1. Mục tiêu tài liệu

Tài liệu này mô tả chi tiết các thành phần phục vụ:

- Quản lý tài khoản người dùng.
- Xác thực đăng nhập.
- Phân quyền theo Role và Permission.
- Quản lý địa chỉ nhận hàng của khách hàng.
- Quên mật khẩu và đặt lại mật khẩu.
- Quản lý phiên đăng nhập bằng Refresh Token trên Redis.
- Ghi nhận lịch sử thao tác bằng Audit Log.

Tài liệu được sử dụng làm đầu vào cho Claude Code hoặc AI coding agent để sinh:

- Database migration.
- Entity.
- Repository.
- Service.
- Controller.
- DTO.
- Mapper.
- Redis session service.
- Security configuration.
- Unit test.
- Integration test.

---

# 2. Phạm vi bảng và thành phần

Các bảng chính:

```text
users
roles
permissions
user_roles
role_permissions
customer_addresses
password_reset_tokens
audit_logs
```

Không tạo bảng `refresh_tokens` trong database.

Refresh token được lưu trên Redis.

Có thể bổ sung bảng sau để lưu lịch sử đăng nhập:

```text
login_histories
```

---

# 3. Mối quan hệ tổng thể

```text
users
 ├── 1:N customer_addresses
 ├── 1:N password_reset_tokens
 ├── 1:N audit_logs
 ├── 1:N login_histories
 └── N:N roles
        └── thông qua user_roles

roles
 └── N:N permissions
        └── thông qua role_permissions

Refresh Token
 └── lưu trên Redis theo user và session
```

Sơ đồ quan hệ:

```text
users
  │
  ├──────────────< customer_addresses
  │
  ├──────────────< password_reset_tokens
  │
  ├──────────────< audit_logs
  │
  ├──────────────< login_histories
  │
  └──────────────< user_roles >────────────── roles
                                              │
                                              └────< role_permissions >──── permissions
```

---

# 3.1. Chuẩn UUID áp dụng

Toàn bộ khóa chính và khóa ngoại trong module này sử dụng UUID.

## Java

```java
import java.util.UUID;

private UUID id;
```

UUID nên được sinh tại application:

```java
UUID id = UUID.randomUUID();
```

Nếu hệ thống có lưu lượng ghi lớn và cần index tuần tự hơn, có thể sử dụng UUIDv7.

## PostgreSQL

```sql
id UUID PRIMARY KEY
```

Có thể đặt mặc định tại database:

```sql
id UUID PRIMARY KEY DEFAULT gen_random_uuid()
```

## MariaDB/MySQL

Tài liệu này sử dụng:

```sql
id CHAR(36) PRIMARY KEY
```

Java vẫn map bằng `java.util.UUID`.

Không sử dụng:

```sql
AUTO_INCREMENT
```

Các foreign key như:

```text
user_id
role_id
permission_id
assigned_by
actor_id
```

cũng sử dụng `CHAR(36)`.

Trường `version` vẫn sử dụng `BIGINT` vì đây là số phiên bản phục vụ optimistic locking, không phải ID.

---

# 4. Quy ước chung cho database

- Database ưu tiên MariaDB hoặc PostgreSQL.
- Primary key và foreign key dùng UUID.
- Trong Java sử dụng `java.util.UUID`.
- Với PostgreSQL sử dụng kiểu `UUID`.
- Với MariaDB/MySQL tài liệu này dùng `CHAR(36)` để dễ đọc và tương thích.
- Có thể tối ưu sang `BINARY(16)` khi cần hiệu năng cao.
- UUID được sinh tại application bằng `UUID.randomUUID()` hoặc UUIDv7.
- Không sử dụng `AUTO_INCREMENT`.
- Các bảng nghiệp vụ có `created_at` và `updated_at`.
- Bảng hỗ trợ soft delete có `deleted_at`.
- Mã nghiệp vụ dùng `VARCHAR` và unique index.
- Trạng thái dùng enum trong Java và `VARCHAR` trong database.
- Không lưu mật khẩu dạng plain text.
- Không lưu refresh token gốc trong database hoặc Redis.
- Không ghi token, mật khẩu hoặc OTP vào audit log.
- Tất cả foreign key cần có index.
- Các trường tìm kiếm thường xuyên cần index.

---

# 5. Bảng `users`

## 5.1. Mục đích nghiệp vụ

Bảng `users` lưu tài khoản dùng để đăng nhập hệ thống.

Bảng này có thể lưu:

- Khách hàng mua hàng.
- Nhân viên quản trị.
- Nhân viên kho.
- Nhân viên xử lý đơn hàng.
- Nhân viên chăm sóc khách hàng.
- Super Admin.
- Tài khoản hệ thống nếu cần.

Không nên tạo riêng bảng đăng nhập cho customer và admin nếu toàn bộ hệ thống dùng chung cơ chế xác thực.

Việc phân biệt loại tài khoản được thực hiện bằng:

- `user_type`.
- Role trong bảng `user_roles`.
- Permission được cấp qua Role.

## 5.2. Nghiệp vụ sử dụng

- Đăng ký tài khoản.
- Đăng nhập.
- Đăng xuất.
- Khóa hoặc mở khóa tài khoản.
- Xác thực email.
- Xác thực số điện thoại.
- Đổi mật khẩu.
- Quên mật khẩu.
- Phân quyền.
- Quản lý hồ sơ cá nhân.
- Ghi nhận số lần đăng nhập sai.
- Ghi nhận thời gian đăng nhập gần nhất.

## 5.3. Field đề xuất

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `username` | VARCHAR(100) | Có | Tên đăng nhập |
| `email` | VARCHAR(255) | Không | Email |
| `phone_number` | VARCHAR(20) | Không | Số điện thoại |
| `password_hash` | VARCHAR(255) | Có | Mật khẩu đã hash |
| `full_name` | VARCHAR(255) | Không | Họ tên |
| `avatar_url` | VARCHAR(1000) | Không | URL ảnh đại diện |
| `user_type` | VARCHAR(30) | Có | Loại người dùng |
| `status` | VARCHAR(30) | Có | Trạng thái tài khoản |
| `email_verified` | BOOLEAN | Có | Email đã xác thực chưa |
| `phone_verified` | BOOLEAN | Có | Số điện thoại đã xác thực chưa |
| `failed_login_attempts` | INT | Có | Số lần đăng nhập sai liên tiếp |
| `locked_until` | DATETIME | Không | Khóa tạm đến thời điểm nào |
| `last_login_at` | DATETIME | Không | Lần đăng nhập gần nhất |
| `password_changed_at` | DATETIME | Không | Thời điểm đổi mật khẩu |
| `created_at` | DATETIME | Có | Thời điểm tạo |
| `updated_at` | DATETIME | Có | Thời điểm cập nhật |
| `deleted_at` | DATETIME | Không | Thời điểm xóa mềm |
| `version` | BIGINT | Có | Optimistic locking |

## 5.4. SQL đề xuất

```sql
CREATE TABLE users (
    id CHAR(36) PRIMARY KEY,

    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NULL,
    phone_number VARCHAR(20) NULL,

    password_hash VARCHAR(255) NOT NULL,

    full_name VARCHAR(255) NULL,
    avatar_url VARCHAR(1000) NULL,

    user_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,

    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME(6) NULL,

    last_login_at DATETIME(6) NULL,
    password_changed_at DATETIME(6) NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone_number UNIQUE (phone_number)
);

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_created_at ON users(created_at);
```

## 5.5. Enum đề xuất

```java
public enum UserType {
    CUSTOMER,
    ADMIN,
    STAFF,
    SYSTEM
}
```

```java
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED,
    DELETED
}
```

## 5.6. Quy tắc nghiệp vụ

- `id` phải được sinh bằng UUID trước khi persist.
- `username` phải duy nhất.
- Email nếu có thì phải duy nhất.
- Số điện thoại nếu có thì phải duy nhất.
- Chỉ lưu `password_hash`.
- Không trả `password_hash` trong API.
- Tài khoản `LOCKED` không được đăng nhập.
- Khi đăng nhập sai, tăng `failed_login_attempts`.
- Khi đăng nhập thành công, reset `failed_login_attempts` về 0 và cập nhật `last_login_at`.
- Khi vượt quá số lần cho phép, khóa tạm bằng `locked_until` hoặc chuyển trạng thái.
- Khi đổi mật khẩu, cập nhật `password_changed_at` và thu hồi toàn bộ refresh session trên Redis.

---

# 6. Bảng `roles`

## 6.1. Mục đích nghiệp vụ

Bảng `roles` lưu vai trò của người dùng.

Role là một nhóm permission.

Ví dụ:

```text
SUPER_ADMIN
ADMIN
PRODUCT_MANAGER
ORDER_MANAGER
WAREHOUSE_STAFF
CUSTOMER_SUPPORT
CUSTOMER
```

## 6.2. Nghiệp vụ sử dụng

- Tạo vai trò.
- Sửa vai trò.
- Khóa hoặc mở vai trò.
- Gán vai trò cho user.
- Gán permission cho role.
- Phân biệt role hệ thống và role tự tạo.

## 6.3. Field đề xuất

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `code` | VARCHAR(100) | Có | Mã role dùng trong code |
| `name` | VARCHAR(255) | Có | Tên hiển thị |
| `description` | VARCHAR(1000) | Không | Mô tả |
| `role_type` | VARCHAR(30) | Có | SYSTEM hoặc CUSTOM |
| `status` | VARCHAR(30) | Có | ACTIVE hoặc INACTIVE |
| `is_system` | BOOLEAN | Có | Role hệ thống, không cho xóa |
| `created_at` | DATETIME | Có | Thời điểm tạo |
| `updated_at` | DATETIME | Có | Thời điểm cập nhật |
| `deleted_at` | DATETIME | Không | Xóa mềm |

## 6.4. SQL đề xuất

```sql
CREATE TABLE roles (
    id CHAR(36) PRIMARY KEY,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,

    role_type VARCHAR(30) NOT NULL DEFAULT 'SYSTEM',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    is_system BOOLEAN NOT NULL DEFAULT FALSE,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    CONSTRAINT uk_roles_code UNIQUE (code)
);

CREATE INDEX idx_roles_status ON roles(status);
CREATE INDEX idx_roles_role_type ON roles(role_type);
```

## 6.5. Enum đề xuất

```java
public enum RoleType {
    SYSTEM,
    CUSTOM
}
```

```java
public enum RoleStatus {
    ACTIVE,
    INACTIVE
}
```

## 6.6. Quy tắc nghiệp vụ

- `code` phải duy nhất.
- Role hệ thống không được xóa.
- Role đang được gán cho user không được xóa cứng.
- Role `INACTIVE` không được gán mới.
- Có thể cho phép clone role.
- Khi thay đổi permission của role, xóa cache quyền nếu có.

---

# 7. Bảng `permissions`

## 7.1. Mục đích nghiệp vụ

Bảng `permissions` lưu quyền chi tiết nhỏ nhất mà backend có thể kiểm tra.

Ví dụ:

```text
PRODUCT_VIEW
PRODUCT_CREATE
PRODUCT_UPDATE
PRODUCT_DELETE
ORDER_VIEW
ORDER_CONFIRM
ORDER_CANCEL
ORDER_UPDATE_STATUS
USER_VIEW
USER_LOCK
USER_ASSIGN_ROLE
```

## 7.2. Nghiệp vụ sử dụng

- Khai báo quyền hệ thống.
- Gán quyền vào role.
- Kiểm tra quyền khi gọi API.
- Ẩn hoặc hiện menu/button ở frontend.
- Phân quyền theo module và hành động.

## 7.3. Field đề xuất

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `code` | VARCHAR(150) | Có | Mã permission |
| `name` | VARCHAR(255) | Có | Tên hiển thị |
| `description` | VARCHAR(1000) | Không | Mô tả |
| `module` | VARCHAR(100) | Có | Module nghiệp vụ |
| `action` | VARCHAR(50) | Có | Hành động |
| `status` | VARCHAR(30) | Có | ACTIVE hoặc INACTIVE |
| `is_system` | BOOLEAN | Có | Permission hệ thống |
| `created_at` | DATETIME | Có | Thời điểm tạo |
| `updated_at` | DATETIME | Có | Thời điểm cập nhật |

## 7.4. SQL đề xuất

```sql
CREATE TABLE permissions (
    id CHAR(36) PRIMARY KEY,

    code VARCHAR(150) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,

    module VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    is_system BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT uk_permissions_code UNIQUE (code)
);

CREATE INDEX idx_permissions_module ON permissions(module);
CREATE INDEX idx_permissions_action ON permissions(action);
CREATE INDEX idx_permissions_status ON permissions(status);
```

## 7.5. Quy tắc đặt mã permission

```text
<MODULE>_<ACTION>
```

Ví dụ:

```text
PRODUCT_VIEW
PRODUCT_CREATE
ORDER_CONFIRM
ROLE_ASSIGN_PERMISSION
```

## 7.6. Cách dùng trong backend

```java
@PreAuthorize("hasAuthority('PRODUCT_CREATE')")
@PostMapping
public ApiResponse<ProductResponse> createProduct(
        @Valid @RequestBody ProductCreateRequest request
) {
    return ApiResponse.success(productService.create(request));
}
```

---

# 8. Bảng `user_roles`

## 8.1. Mục đích nghiệp vụ

Bảng `user_roles` là bảng trung gian giữa `users` và `roles`.

Một user có thể có nhiều role. Một role có thể được gán cho nhiều user.

## 8.2. Nghiệp vụ sử dụng

- Gán role cho user.
- Thu hồi role.
- Gán role có thời hạn.
- Ghi nhận người thực hiện gán role.
- Khóa tạm một role của user.

## 8.3. Field đề xuất

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `user_id` | UUID | Có | User được gán role |
| `role_id` | UUID | Có | Role được gán |
| `assigned_by` | UUID | Không | User thực hiện gán |
| `assigned_at` | DATETIME | Có | Thời điểm gán |
| `effective_from` | DATETIME | Không | Bắt đầu hiệu lực |
| `effective_to` | DATETIME | Không | Hết hiệu lực |
| `status` | VARCHAR(30) | Có | ACTIVE hoặc INACTIVE |
| `created_at` | DATETIME | Có | Thời điểm tạo |
| `updated_at` | DATETIME | Có | Thời điểm cập nhật |

## 8.4. SQL đề xuất

```sql
CREATE TABLE user_roles (
    id CHAR(36) PRIMARY KEY,

    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,

    assigned_by CHAR(36) NULL,
    assigned_at DATETIME(6) NOT NULL,

    effective_from DATETIME(6) NULL,
    effective_to DATETIME(6) NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id),

    CONSTRAINT fk_user_roles_assigned_by
        FOREIGN KEY (assigned_by) REFERENCES users(id),

    CONSTRAINT uk_user_roles_user_role
        UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_user_roles_status ON user_roles(status);
```

## 8.5. Quy tắc nghiệp vụ

- Không gán trùng cùng một role cho cùng một user.
- Không gán role `INACTIVE`.
- Có thể dùng `effective_from` và `effective_to` cho quyền tạm thời.
- Mọi thao tác gán hoặc thu hồi role phải tạo audit log.
- Không cho admin tự gỡ role cuối cùng của chính mình nếu hệ thống không còn Super Admin.

---

# 9. Bảng `role_permissions`

## 9.1. Mục đích nghiệp vụ

Bảng `role_permissions` là bảng trung gian giữa `roles` và `permissions`.

Một role có nhiều permission. Một permission có thể thuộc nhiều role.

## 9.2. Nghiệp vụ sử dụng

- Gán permission cho role.
- Thu hồi permission khỏi role.
- Ghi nhận người thực hiện.
- Khóa tạm permission trong role.

## 9.3. Field đề xuất

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `role_id` | UUID | Có | Role |
| `permission_id` | UUID | Có | Permission |
| `assigned_by` | UUID | Không | Người gán |
| `assigned_at` | DATETIME | Có | Thời điểm gán |
| `status` | VARCHAR(30) | Có | ACTIVE hoặc INACTIVE |
| `created_at` | DATETIME | Có | Thời điểm tạo |
| `updated_at` | DATETIME | Có | Thời điểm cập nhật |

## 9.4. SQL đề xuất

```sql
CREATE TABLE role_permissions (
    id CHAR(36) PRIMARY KEY,

    role_id CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,

    assigned_by CHAR(36) NULL,
    assigned_at DATETIME(6) NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles(id),

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id),

    CONSTRAINT fk_role_permissions_assigned_by
        FOREIGN KEY (assigned_by) REFERENCES users(id),

    CONSTRAINT uk_role_permissions_role_permission
        UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX idx_role_permissions_status ON role_permissions(status);
```

## 9.5. Quy tắc nghiệp vụ

- Không gán permission trùng trong cùng role.
- Không gán permission `INACTIVE`.
- Permission hệ thống không được xóa cứng.
- Mọi thay đổi role-permission phải ghi audit log.
- Sau khi cập nhật permission, phải xóa cache quyền của role hoặc user nếu có.

---

# 10. Bảng `customer_addresses`

## 10.1. Mục đích nghiệp vụ

Bảng `customer_addresses` lưu danh sách địa chỉ nhận hàng của khách hàng.

Một khách hàng có thể có nhiều địa chỉ như nhà riêng, công ty hoặc địa chỉ tạm trú.

Không nên lưu địa chỉ trực tiếp trong `users` vì một user có thể có nhiều địa chỉ.

## 10.2. Nghiệp vụ sử dụng

- Thêm địa chỉ nhận hàng.
- Sửa địa chỉ.
- Xóa địa chỉ.
- Đặt địa chỉ mặc định.
- Chọn địa chỉ khi checkout.
- Tính phí vận chuyển.
- Tích hợp bản đồ hoặc geocoding.

## 10.3. Field đề xuất

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `user_id` | UUID | Có | Chủ sở hữu |
| `recipient_name` | VARCHAR(255) | Có | Người nhận |
| `phone_number` | VARCHAR(20) | Có | Số điện thoại nhận hàng |
| `province_code` | VARCHAR(20) | Có | Mã tỉnh/thành |
| `province_name` | VARCHAR(255) | Có | Tên tỉnh/thành |
| `district_code` | VARCHAR(20) | Có | Mã quận/huyện |
| `district_name` | VARCHAR(255) | Có | Tên quận/huyện |
| `ward_code` | VARCHAR(20) | Có | Mã phường/xã |
| `ward_name` | VARCHAR(255) | Có | Tên phường/xã |
| `address_line` | VARCHAR(1000) | Có | Số nhà, đường, ngõ |
| `address_type` | VARCHAR(30) | Có | HOME, OFFICE, OTHER |
| `label` | VARCHAR(100) | Không | Nhãn hiển thị |
| `is_default` | BOOLEAN | Có | Địa chỉ mặc định |
| `latitude` | DECIMAL(10,7) | Không | Vĩ độ |
| `longitude` | DECIMAL(10,7) | Không | Kinh độ |
| `status` | VARCHAR(30) | Có | ACTIVE hoặc INACTIVE |
| `created_at` | DATETIME | Có | Thời điểm tạo |
| `updated_at` | DATETIME | Có | Thời điểm cập nhật |
| `deleted_at` | DATETIME | Không | Xóa mềm |

## 10.4. SQL đề xuất

```sql
CREATE TABLE customer_addresses (
    id CHAR(36) PRIMARY KEY,

    user_id CHAR(36) NOT NULL,

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

    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    CONSTRAINT fk_customer_addresses_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_customer_addresses_user_id ON customer_addresses(user_id);
CREATE INDEX idx_customer_addresses_is_default ON customer_addresses(user_id, is_default);
CREATE INDEX idx_customer_addresses_status ON customer_addresses(status);
```

## 10.5. Enum đề xuất

```java
public enum AddressType {
    HOME,
    OFFICE,
    OTHER
}
```

## 10.6. Quy tắc nghiệp vụ

- Mỗi user chỉ có tối đa một địa chỉ mặc định.
- Khi đặt một địa chỉ thành mặc định, cập nhật tất cả địa chỉ khác thành `is_default = false`.
- Khi xóa địa chỉ mặc định, chọn địa chỉ còn lại gần nhất làm mặc định nếu có.
- Khi tạo đơn hàng, copy dữ liệu địa chỉ sang bảng `order_addresses`.
- Không chỉ lưu `address_id` trong đơn hàng vì khách có thể sửa địa chỉ sau khi đặt hàng.

---

# 11. Bảng `password_reset_tokens`

## 11.1. Mục đích nghiệp vụ

Bảng này phục vụ chức năng quên mật khẩu và đặt lại mật khẩu.

Luồng:

```text
Người dùng nhập email hoặc username
→ Backend tìm user
→ Sinh token ngẫu nhiên
→ Hash token
→ Lưu token hash vào database
→ Gửi link reset qua email
→ Người dùng mở link
→ Backend hash token nhận được
→ So sánh với token hash
→ Cho phép đặt mật khẩu mới
→ Đánh dấu token đã sử dụng
```

## 11.2. Nghiệp vụ sử dụng

- Yêu cầu quên mật khẩu.
- Xác nhận token.
- Đặt lại mật khẩu.
- Vô hiệu hóa token.
- Giới hạn thời gian sử dụng.
- Ghi nhận IP và thiết bị yêu cầu.

## 11.3. Field đề xuất

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `user_id` | UUID | Có | User yêu cầu reset |
| `token_hash` | VARCHAR(255) | Có | Hash token |
| `expires_at` | DATETIME | Có | Hạn sử dụng |
| `used_at` | DATETIME | Không | Thời điểm sử dụng |
| `status` | VARCHAR(30) | Có | Trạng thái token |
| `requested_ip` | VARCHAR(50) | Không | IP yêu cầu |
| `user_agent` | VARCHAR(1000) | Không | Trình duyệt/thiết bị |
| `created_at` | DATETIME | Có | Thời điểm tạo |

## 11.4. SQL đề xuất

```sql
CREATE TABLE password_reset_tokens (
    id CHAR(36) PRIMARY KEY,

    user_id CHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,

    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    requested_ip VARCHAR(50) NULL,
    user_agent VARCHAR(1000) NULL,

    created_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT uk_password_reset_tokens_hash
        UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_status ON password_reset_tokens(status);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);
```

## 11.5. Enum đề xuất

```java
public enum ResetTokenStatus {
    ACTIVE,
    USED,
    EXPIRED,
    REVOKED
}
```

## 11.6. Quy tắc bảo mật

- Không lưu token gốc.
- Lưu `SHA-256(token)` hoặc hash phù hợp.
- Token chỉ dùng một lần.
- Token nên hết hạn sau 15–30 phút.
- Khi reset thành công, cập nhật token thành `USED`, đổi mật khẩu và thu hồi toàn bộ Redis session.
- Không cho biết email có tồn tại hay không trong response để tránh dò tài khoản.
- Có rate limit cho API quên mật khẩu.

---

# 12. Refresh Token lưu trên Redis

## 12.1. Quyết định thiết kế

Không tạo bảng `refresh_tokens` trong database.

Refresh token/session đang hoạt động được lưu trên Redis.

Database chỉ lưu:

- User.
- Lịch sử đăng nhập.
- Audit log.

Redis lưu:

- Refresh session đang hoạt động.
- Token hash.
- Session ID.
- Device.
- TTL.
- Token family.

## 12.2. Mục đích nghiệp vụ

- Giữ phiên đăng nhập lâu dài.
- Cấp access token mới.
- Logout một thiết bị.
- Logout tất cả thiết bị.
- Thu hồi session khi đổi mật khẩu.
- Thu hồi session khi khóa user.
- Phát hiện reuse refresh token.
- Tự động hết hạn bằng TTL.

## 12.3. Access token

```text
Loại: JWT
Thời hạn: 10–15 phút
Không lưu database
```

## 12.4. Refresh token

```text
Thời hạn: 7–30 ngày
Gửi client: HttpOnly Secure Cookie
Lưu server: hash trên Redis
Sử dụng rotation
```

## 12.5. Không lưu token gốc

Không lưu:

```text
refreshToken = raw-token-value
```

Nên lưu:

```text
tokenHash = SHA-256(refreshToken)
```

## 12.6. Redis key đề xuất

### Session key

```text
ecommerce:auth:session:{sessionId}
```

### User session set

```text
ecommerce:auth:user-sessions:{userId}
```

### Token lookup key

```text
ecommerce:auth:refresh:{tokenHash}
```

Giá trị của token lookup key là `sessionId`.

## 12.7. Redis session value

```json
{
  "sessionId": "7f9d0e3a-28f7-4d74-8d5a-4b14a7ac9001",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "customer01",
  "refreshTokenHash": "b3f2d41f...",
  "tokenFamily": "0c93d476-4ef5-41e8-8fc0-7faad8500133",
  "deviceId": "chrome-windows-001",
  "deviceName": "Chrome on Windows",
  "ipAddress": "192.168.1.10",
  "userAgent": "Mozilla/5.0 ...",
  "issuedAt": "2026-07-28T14:00:00Z",
  "expiresAt": "2026-08-04T14:00:00Z",
  "lastUsedAt": "2026-07-28T14:00:00Z",
  "status": "ACTIVE"
}
```

## 12.8. Redis TTL

Ví dụ 7 ngày:

```text
604800 giây
```

Khi tạo session:

```text
SET ecommerce:auth:session:{sessionId} <json> EX 604800
SET ecommerce:auth:refresh:{tokenHash} {sessionId} EX 604800
SADD ecommerce:auth:user-sessions:{userId} {sessionId}
```

## 12.9. Luồng đăng nhập

```text
1. Validate username/password.
2. Kiểm tra trạng thái user.
3. Sinh access token.
4. Sinh refresh token ngẫu nhiên.
5. Hash refresh token.
6. Sinh sessionId.
7. Sinh tokenFamily.
8. Lưu session vào Redis.
9. Lưu mapping tokenHash → sessionId.
10. Thêm sessionId vào set session của user.
11. Trả access token.
12. Đặt refresh token vào HttpOnly Secure Cookie.
13. Ghi login history.
```

## 12.10. Luồng refresh token

```text
1. Đọc refresh token từ HttpOnly Cookie.
2. Hash refresh token.
3. Tìm sessionId bằng tokenHash trên Redis.
4. Lấy session.
5. Kiểm tra TTL và trạng thái.
6. Kiểm tra user còn ACTIVE.
7. Xóa token hash cũ.
8. Sinh refresh token mới.
9. Hash token mới.
10. Cập nhật session.
11. Lưu token hash mới.
12. Reset TTL.
13. Sinh access token mới.
14. Trả access token mới.
15. Ghi refresh event nếu cần.
```

## 12.11. Refresh token rotation

Mỗi refresh token chỉ dùng một lần.

```text
Refresh token cũ
→ validate
→ revoke/xóa
→ sinh refresh token mới
```

Nếu token cũ bị sử dụng lại:

```text
Không tìm thấy tokenHash
→ coi là nghi ngờ token bị đánh cắp
→ revoke toàn bộ tokenFamily hoặc toàn bộ session của user
→ yêu cầu đăng nhập lại
```

## 12.12. Luồng logout một thiết bị

```text
1. Đọc refresh token.
2. Hash token.
3. Tìm sessionId.
4. Xóa session key.
5. Xóa token lookup key.
6. Xóa sessionId khỏi user session set.
7. Xóa cookie.
8. Ghi login history.
```

## 12.13. Luồng logout tất cả thiết bị

```text
1. Lấy danh sách sessionId theo userId.
2. Xóa toàn bộ session key.
3. Xóa toàn bộ token lookup key.
4. Xóa user session set.
5. Ghi login history.
```

## 12.14. Khi khóa user

```text
1. Thu hồi toàn bộ session trên Redis.
2. Không cho refresh token.
3. Không cho login mới.
4. Ghi audit log.
```

## 12.15. Khi đổi mật khẩu

```text
1. Validate mật khẩu cũ.
2. Hash mật khẩu mới.
3. Cập nhật password_hash.
4. Cập nhật password_changed_at.
5. Thu hồi toàn bộ session trên Redis.
6. Yêu cầu đăng nhập lại.
7. Ghi audit log.
```

## 12.16. Redis configuration yêu cầu

- Redis Sentinel hoặc Redis Cluster.
- Redis authentication.
- Không public Redis ra Internet.
- Chỉ application được truy cập.
- Bật TLS nếu hạ tầng hỗ trợ.
- Có replication.
- Có persistence phù hợp.
- Khuyến nghị `noeviction` cho Redis quản lý session.
- Có metric theo dõi memory.
- Có cảnh báo khi gần đầy bộ nhớ.

## 12.17. Java model gợi ý

```java
import java.util.UUID;

public record RefreshSession(
        String sessionId,
        UUID userId,
        String username,
        String refreshTokenHash,
        String tokenFamily,
        String deviceId,
        String deviceName,
        String ipAddress,
        String userAgent,
        Instant issuedAt,
        Instant expiresAt,
        Instant lastUsedAt,
        RefreshSessionStatus status
) {
}
```

```java
public enum RefreshSessionStatus {
    ACTIVE,
    REVOKED
}
```

## 12.18. Service interface gợi ý

```java
public interface RefreshSessionService {

    RefreshSession createSession(
            User user,
            String rawRefreshToken,
            DeviceInfo deviceInfo
    );

    RefreshSession validate(String rawRefreshToken);

    RefreshSession rotate(
            String rawRefreshToken,
            String newRawRefreshToken
    );

    void revokeByRefreshToken(String rawRefreshToken);

    void revokeSession(String sessionId);

    void revokeAllByUserId(UUID userId);

    List<RefreshSession> getActiveSessions(UUID userId);
}
```

---

# 13. Bảng `audit_logs`

## 13.1. Mục đích nghiệp vụ

Audit log ghi lại:

```text
Ai
đã làm gì
trên dữ liệu nào
vào thời điểm nào
kết quả ra sao
```

Audit log khác application log.

### Application log

Phục vụ debug kỹ thuật:

```text
NullPointerException
Database timeout
Kafka connection failed
```

### Audit log

Phục vụ truy vết nghiệp vụ:

```text
Admin A sửa giá sản phẩm.
Admin B hủy đơn hàng.
Admin C gán role cho nhân viên.
Nhân viên kho điều chỉnh tồn kho.
```

## 13.2. Nghiệp vụ cần audit

- Đăng nhập admin.
- Đăng nhập thất bại.
- Khóa hoặc mở khóa tài khoản.
- Gán hoặc thu hồi role.
- Gán hoặc thu hồi permission.
- Tạo, sửa, xóa sản phẩm.
- Thay đổi giá.
- Điều chỉnh tồn kho.
- Xác nhận hoặc hủy đơn.
- Hoàn tiền.
- Thay đổi cấu hình hệ thống.

## 13.3. Field đề xuất

| Field | Kiểu dữ liệu | Bắt buộc | Ý nghĩa |
|---|---|---:|---|
| `id` | UUID | Có | Khóa chính |
| `actor_id` | UUID | Không | Người thực hiện |
| `actor_username` | VARCHAR(100) | Không | Snapshot username |
| `actor_type` | VARCHAR(30) | Có | CUSTOMER, ADMIN, SYSTEM |
| `action` | VARCHAR(100) | Có | Hành động |
| `module` | VARCHAR(100) | Có | Module |
| `object_type` | VARCHAR(100) | Không | Loại đối tượng |
| `object_id` | VARCHAR(100) | Không | ID đối tượng |
| `description` | VARCHAR(2000) | Không | Mô tả |
| `old_value` | JSON/LONGTEXT | Không | Dữ liệu cũ |
| `new_value` | JSON/LONGTEXT | Không | Dữ liệu mới |
| `request_method` | VARCHAR(10) | Không | HTTP method |
| `request_path` | VARCHAR(1000) | Không | API path |
| `ip_address` | VARCHAR(50) | Không | IP |
| `user_agent` | VARCHAR(1000) | Không | Thiết bị |
| `trace_id` | VARCHAR(100) | Không | Trace ID |
| `status` | VARCHAR(30) | Có | SUCCESS hoặc FAILED |
| `error_code` | VARCHAR(100) | Không | Mã lỗi |
| `error_message` | VARCHAR(2000) | Không | Nội dung lỗi |
| `created_at` | DATETIME | Có | Thời điểm ghi |

## 13.4. SQL đề xuất

```sql
CREATE TABLE audit_logs (
    id CHAR(36) PRIMARY KEY,

    actor_id CHAR(36) NULL,
    actor_username VARCHAR(100) NULL,
    actor_type VARCHAR(30) NOT NULL,

    action VARCHAR(100) NOT NULL,
    module VARCHAR(100) NOT NULL,

    object_type VARCHAR(100) NULL,
    object_id VARCHAR(100) NULL,

    description VARCHAR(2000) NULL,

    old_value LONGTEXT NULL,
    new_value LONGTEXT NULL,

    request_method VARCHAR(10) NULL,
    request_path VARCHAR(1000) NULL,

    ip_address VARCHAR(50) NULL,
    user_agent VARCHAR(1000) NULL,

    trace_id VARCHAR(100) NULL,
    status VARCHAR(30) NOT NULL,

    error_code VARCHAR(100) NULL,
    error_message VARCHAR(2000) NULL,

    created_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_audit_logs_actor
        FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_module ON audit_logs(module);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_object ON audit_logs(object_type, object_id);
CREATE INDEX idx_audit_logs_trace_id ON audit_logs(trace_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
```

## 13.5. Enum đề xuất

```java
public enum AuditActorType {
    CUSTOMER,
    ADMIN,
    STAFF,
    SYSTEM
}
```

```java
public enum AuditStatus {
    SUCCESS,
    FAILED
}
```

```java
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    LOCK,
    UNLOCK,
    ASSIGN_ROLE,
    REMOVE_ROLE,
    ASSIGN_PERMISSION,
    REMOVE_PERMISSION,
    CHANGE_PASSWORD,
    RESET_PASSWORD
}
```

## 13.6. Quy tắc bảo mật

Không ghi vào audit log:

```text
password
password_hash
access_token
refresh_token
OTP
CVV
thông tin thẻ ngân hàng
secret key
```

Khi lưu `old_value` và `new_value`:

- Mask dữ liệu nhạy cảm.
- Chỉ lưu field cần thiết.
- Không serialize toàn bộ entity một cách thiếu kiểm soát.

---

# 14. Bảng `login_histories` đề xuất bổ sung

## 14.1. Mục đích

Vì refresh token nằm trên Redis và có thể hết hạn hoặc bị xóa, bảng `login_histories` dùng để lưu lịch sử đăng nhập lâu dài.

## 14.2. Nghiệp vụ sử dụng

- Xem lịch sử đăng nhập.
- Xem thiết bị đăng nhập.
- Điều tra đăng nhập bất thường.
- Ghi nhận login fail.
- Ghi nhận logout.
- Ghi nhận revoke session.
- Phục vụ báo cáo và audit.

## 14.3. Field đề xuất

| Field | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| `id` | UUID | Khóa chính |
| `user_id` | UUID | User |
| `username` | VARCHAR(100) | Snapshot username |
| `action` | VARCHAR(30) | LOGIN, LOGOUT... |
| `status` | VARCHAR(30) | SUCCESS, FAILED |
| `session_id` | VARCHAR(100) | Session ID |
| `device_id` | VARCHAR(255) | Mã thiết bị |
| `device_name` | VARCHAR(255) | Tên thiết bị |
| `ip_address` | VARCHAR(50) | IP |
| `user_agent` | VARCHAR(1000) | Trình duyệt |
| `failure_reason` | VARCHAR(1000) | Lý do thất bại |
| `trace_id` | VARCHAR(100) | Trace ID |
| `created_at` | DATETIME | Thời điểm |

## 14.4. SQL đề xuất

```sql
CREATE TABLE login_histories (
    id CHAR(36) PRIMARY KEY,

    user_id CHAR(36) NULL,
    username VARCHAR(100) NULL,

    action VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,

    session_id VARCHAR(100) NULL,
    device_id VARCHAR(255) NULL,
    device_name VARCHAR(255) NULL,

    ip_address VARCHAR(50) NULL,
    user_agent VARCHAR(1000) NULL,

    failure_reason VARCHAR(1000) NULL,
    trace_id VARCHAR(100) NULL,

    created_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_login_histories_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_login_histories_user_id ON login_histories(user_id);
CREATE INDEX idx_login_histories_action ON login_histories(action);
CREATE INDEX idx_login_histories_status ON login_histories(status);
CREATE INDEX idx_login_histories_created_at ON login_histories(created_at);
```

## 14.5. Enum đề xuất

```java
public enum LoginAction {
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    LOGOUT_ALL,
    REFRESH_TOKEN,
    TOKEN_REVOKED,
    PASSWORD_CHANGED,
    PASSWORD_RESET
}
```

---

# 15. Luồng phân quyền hoàn chỉnh

Ví dụ:

```text
User: employee01
Role: ORDER_MANAGER
Permissions:
- ORDER_VIEW
- ORDER_CONFIRM
- ORDER_CANCEL
```

Dữ liệu:

```text
users
└── employee01

user_roles
└── employee01 → ORDER_MANAGER

roles
└── ORDER_MANAGER

role_permissions
└── ORDER_MANAGER → ORDER_VIEW
└── ORDER_MANAGER → ORDER_CONFIRM
└── ORDER_MANAGER → ORDER_CANCEL
```

Khi user gọi:

```http
POST /api/v1/admin/orders/100/confirm
```

Backend kiểm tra:

```text
1. Access token hợp lệ.
2. User còn ACTIVE.
3. User có role đang ACTIVE.
4. Role có permission ORDER_CONFIRM.
5. Cho phép thực hiện.
6. Ghi audit log.
```

---

# 16. Các API đề xuất

## 16.1. Authentication

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
POST /api/v1/auth/logout-all
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
POST /api/v1/auth/change-password
GET  /api/v1/auth/sessions
DELETE /api/v1/auth/sessions/{sessionId}
```

## 16.2. User

```text
GET    /api/v1/admin/users
GET    /api/v1/admin/users/{id}
POST   /api/v1/admin/users
PUT    /api/v1/admin/users/{id}
POST   /api/v1/admin/users/{id}/lock
POST   /api/v1/admin/users/{id}/unlock
POST   /api/v1/admin/users/{id}/roles
DELETE /api/v1/admin/users/{id}/roles/{roleId}
```

## 16.3. Role

```text
GET    /api/v1/admin/roles
GET    /api/v1/admin/roles/{id}
POST   /api/v1/admin/roles
PUT    /api/v1/admin/roles/{id}
DELETE /api/v1/admin/roles/{id}
POST   /api/v1/admin/roles/{id}/permissions
DELETE /api/v1/admin/roles/{id}/permissions/{permissionId}
```

## 16.4. Permission

```text
GET /api/v1/admin/permissions
GET /api/v1/admin/permissions/{id}
```

## 16.5. Customer address

```text
GET    /api/v1/customer/addresses
POST   /api/v1/customer/addresses
PUT    /api/v1/customer/addresses/{id}
DELETE /api/v1/customer/addresses/{id}
POST   /api/v1/customer/addresses/{id}/set-default
```

## 16.6. Audit

```text
GET /api/v1/admin/audit-logs
GET /api/v1/admin/audit-logs/{id}
```

---

# 17. Package structure đề xuất

```text
com.example.ecommerce
├── auth
│   ├── controller
│   ├── dto
│   ├── service
│   ├── security
│   ├── token
│   └── redis
│
├── user
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   ├── service
│   └── mapper
│
├── authorization
│   ├── role
│   ├── permission
│   └── security
│
├── address
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   ├── service
│   └── mapper
│
├── audit
│   ├── aspect
│   ├── entity
│   ├── repository
│   ├── service
│   └── annotation
│
├── common
│   ├── exception
│   ├── response
│   ├── config
│   ├── constant
│   └── util
│
└── infrastructure
    ├── redis
    ├── mail
    └── persistence
```

---

# 18. Yêu cầu sinh code cho Claude Code

Claude Code phải sinh đầy đủ:

## Database

- Flyway migration (versioned SQL, `V{n}__mo_ta.sql`).
- Table.
- Foreign key.
- Unique constraint.
- Index.
- Dữ liệu seed cho role và permission mặc định.

## Backend

- Entity.
- Enum.
- Repository.
- Service interface.
- Service implementation.
- Controller.
- Request DTO.
- Response DTO.
- Mapper.
- Validation.
- Exception.
- Error code.
- Global exception handler.
- Security config.
- JWT provider.
- Redis session service.
- Refresh token rotation.
- Login history.
- Audit logging.

## Test

- Unit test.
- Repository test.
- Integration test.
- Security authorization test.
- Redis session test.
- Refresh token rotation test.

---

# 19. Seed dữ liệu đề xuất

## Role

```text
SUPER_ADMIN
ADMIN
PRODUCT_MANAGER
ORDER_MANAGER
WAREHOUSE_STAFF
CUSTOMER_SUPPORT
CUSTOMER
```

## Permission

```text
USER_VIEW
USER_CREATE
USER_UPDATE
USER_LOCK
USER_UNLOCK
USER_ASSIGN_ROLE
ROLE_VIEW
ROLE_CREATE
ROLE_UPDATE
ROLE_DELETE
ROLE_ASSIGN_PERMISSION
PERMISSION_VIEW
ADDRESS_VIEW
ADDRESS_CREATE
ADDRESS_UPDATE
ADDRESS_DELETE
AUDIT_LOG_VIEW
PRODUCT_VIEW
PRODUCT_CREATE
PRODUCT_UPDATE
PRODUCT_DELETE
ORDER_VIEW
ORDER_CONFIRM
ORDER_CANCEL
ORDER_UPDATE_STATUS
INVENTORY_VIEW
INVENTORY_IMPORT
INVENTORY_EXPORT
INVENTORY_ADJUST
```

## Role mapping ví dụ

```text
SUPER_ADMIN
→ toàn bộ permission

PRODUCT_MANAGER
→ PRODUCT_VIEW
→ PRODUCT_CREATE
→ PRODUCT_UPDATE
→ PRODUCT_DELETE

ORDER_MANAGER
→ ORDER_VIEW
→ ORDER_CONFIRM
→ ORDER_CANCEL
→ ORDER_UPDATE_STATUS

WAREHOUSE_STAFF
→ INVENTORY_VIEW
→ INVENTORY_IMPORT
→ INVENTORY_EXPORT
→ INVENTORY_ADJUST

CUSTOMER
→ quyền customer API, không dùng admin permission
```

---

# 20. Test case bắt buộc

## User

- Đăng ký thành công.
- Username trùng.
- Email trùng.
- Phone trùng.
- Login sai mật khẩu.
- Login tài khoản bị khóa.
- Tự động khóa sau nhiều lần login sai.
- Đổi mật khẩu thành công.
- Đổi mật khẩu thu hồi toàn bộ session.

## Role và Permission

- Tạo role.
- Gán permission.
- Gán role cho user.
- Không gán role trùng.
- Không gán permission trùng.
- Không gán role inactive.
- Không gán permission inactive.
- User không có permission bị trả 403.
- User có permission được truy cập.

## Address

- Tạo địa chỉ.
- Đặt địa chỉ mặc định.
- Chỉ có một địa chỉ mặc định.
- Không sửa địa chỉ của user khác.
- Không xóa địa chỉ của user khác.

## Password Reset

- Yêu cầu reset thành công.
- Token hết hạn.
- Token đã dùng.
- Token sai.
- Reset thành công thu hồi toàn bộ Redis session.

## Refresh Session Redis

- Tạo session.
- Refresh thành công.
- Rotation token.
- Token cũ không dùng lại được.
- Logout một thiết bị.
- Logout tất cả thiết bị.
- Khóa user thu hồi session.
- Redis key có TTL đúng.
- Không lưu raw refresh token.

## Audit

- Ghi log khi gán role.
- Ghi log khi thu hồi role.
- Ghi log khi khóa user.
- Ghi log khi reset mật khẩu.
- Không ghi password hoặc token vào log.

---

# 21. Definition of Done

Module hoàn thành khi:

- Có Flyway migration.
- Có seed role và permission.
- Có entity và repository.
- Có service và controller.
- Có DTO và mapper.
- Có validation.
- Có JWT authentication.
- Có authorization theo permission.
- Có refresh session trên Redis.
- Có refresh token rotation.
- Có logout một thiết bị.
- Có logout tất cả thiết bị.
- Có password reset.
- Có address management.
- Có audit log.
- Có login history.
- Có test.
- Không lưu raw password.
- Không lưu raw refresh token.
- Không trả dữ liệu nhạy cảm.
- Code compile thành công.
- Test chạy thành công.

---

# 22. Kết luận

Thiết kế được chốt:

```text
Database:
- users
- roles
- permissions
- user_roles
- role_permissions
- customer_addresses
- password_reset_tokens
- audit_logs
- login_histories

Redis:
- refresh session
- refresh token hash
- token family
- user session set
- TTL
```

Không tạo bảng `refresh_tokens` trong database ở giai đoạn đầu.

Redis là nơi lưu session đang hoạt động.

Database lưu dữ liệu bền vững và lịch sử:

```text
User
Role
Permission
Address
Password reset
Audit
Login history
```

Các nguyên tắc quan trọng nhất:

```text
- Không lưu mật khẩu dạng plain text.
- Không lưu refresh token gốc.
- Refresh token sử dụng rotation.
- Đổi mật khẩu hoặc khóa user phải revoke toàn bộ session.
- Backend kiểm tra permission, không chỉ frontend.
- Mọi thay đổi quyền quan trọng phải có audit log.
```
