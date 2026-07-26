CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

INSERT INTO roles (code, description) VALUES
    ('CUSTOMER', 'Vai tro mac dinh khi dang ky'),
    ('ADMIN', 'Toan quyen quan tri he thong');

INSERT INTO permissions (code, description) VALUES
    ('PRODUCT_CREATE', 'Tao san pham'),
    ('PRODUCT_UPDATE', 'Sua san pham'),
    ('PRODUCT_DELETE', 'Xoa san pham'),
    ('CATEGORY_MANAGE', 'Tao/sua/xoa danh muc'),
    ('ORDER_VIEW_ALL', 'Xem toan bo don hang'),
    ('ORDER_UPDATE_STATUS', 'Cap nhat trang thai don'),
    ('REVIEW_DELETE_ANY', 'Xoa review cua bat ky ai'),
    ('USER_MANAGE', 'Xem/khoa tai khoan nguoi dung khac'),
    ('ROLE_MANAGE', 'Quan ly role va permission');

-- ADMIN duoc gan toan bo permission hien co
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'ADMIN'), id FROM permissions;

-- Migrate du lieu cu: moi user da dang ky truoc day duoc gan role tuong ung
-- theo cot `role` cu (VD user.role = 'ADMIN' -> gan role_id cua roles.code = 'ADMIN')
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.code = u.role;
