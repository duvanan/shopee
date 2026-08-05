-- Mo rong schema users/roles/permissions/user_roles/role_permissions theo
-- docs/architecture/user-auth-role-permission-redis-refresh-token-design-uuid.md.
-- Chi lay phan cot co hanh vi thuc su ngay trong phase nay (auto-lock dang
-- nhap sai nhieu lan, ten hien thi cho role/permission, danh dau role/permission
-- he thong). Cac cot con lai cua thiet ke goc (user_type, email_verified,
-- deleted_at, version, status/effective_from/effective_to cua user_roles...)
-- se them cung luc voi tinh nang tuong ung (quan ly user cua admin, xac thuc
-- email, doi mat khau...) o phase sau, tranh cot mo côi khong ai doc/ghi.

ALTER TABLE users
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMPTZ NULL,
    ADD COLUMN last_login_at TIMESTAMPTZ NULL;

ALTER TABLE roles
    ADD COLUMN name VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE roles SET name = 'Khách hàng', is_system = TRUE WHERE code = 'CUSTOMER';
UPDATE roles SET name = 'Quản trị viên', is_system = TRUE WHERE code = 'ADMIN';

ALTER TABLE roles ALTER COLUMN name DROP DEFAULT;

ALTER TABLE permissions
    ADD COLUMN name VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE permissions SET name = CASE code
    WHEN 'PRODUCT_CREATE' THEN 'Tạo sản phẩm'
    WHEN 'PRODUCT_UPDATE' THEN 'Sửa sản phẩm'
    WHEN 'PRODUCT_DELETE' THEN 'Xóa sản phẩm'
    WHEN 'CATEGORY_MANAGE' THEN 'Quản lý danh mục'
    WHEN 'ORDER_VIEW_ALL' THEN 'Xem toàn bộ đơn hàng'
    WHEN 'ORDER_UPDATE_STATUS' THEN 'Cập nhật trạng thái đơn'
    WHEN 'REVIEW_DELETE_ANY' THEN 'Xóa review của bất kỳ ai'
    WHEN 'USER_MANAGE' THEN 'Xem/khóa tài khoản người dùng khác'
    WHEN 'ROLE_MANAGE' THEN 'Quản lý role và permission'
    ELSE code
END;

ALTER TABLE permissions ALTER COLUMN name DROP DEFAULT;

-- assigned_at: moc thoi gian gan role/permission - gan lien voi ban chat
-- cua chinh dong du lieu (mot lan gan quyen), khong phai tinh nang rieng
-- nen them ngay, khac voi effective_from/effective_to/status (chinh sach
-- quyen co thoi han - chua co nhu cau, de danh cho phase quan ly role sau).
ALTER TABLE user_roles
    ADD COLUMN assigned_by UUID NULL,
    ADD COLUMN assigned_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE role_permissions
    ADD COLUMN assigned_by UUID NULL,
    ADD COLUMN assigned_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX idx_users_locked_until ON users(locked_until);
