-- module: nhom permission theo nghiep vu de UI phan quyen hien thi theo
-- section thay vi 1 danh sach phang - dung khi so permission tang len theo
-- thoi gian (them permission moi chi can dat dung module, UI tu gom nhom,
-- khong phai sua code frontend). Xem AdminRoleFormView.vue.
ALTER TABLE permissions ADD COLUMN module VARCHAR(100) NOT NULL DEFAULT 'Khác';

UPDATE permissions SET module = CASE
    WHEN code LIKE 'PRODUCT_%' THEN 'Sản phẩm'
    WHEN code = 'CATEGORY_MANAGE' THEN 'Danh mục'
    WHEN code LIKE 'ORDER_%' THEN 'Đơn hàng'
    WHEN code LIKE 'REVIEW_%' THEN 'Đánh giá'
    WHEN code = 'USER_MANAGE' THEN 'Người dùng'
    WHEN code = 'ROLE_MANAGE' THEN 'Phân quyền'
    WHEN code = 'AUDIT_LOG_VIEW' THEN 'Hệ thống'
    ELSE 'Khác'
END;

ALTER TABLE permissions ALTER COLUMN module DROP DEFAULT;

CREATE INDEX idx_permissions_module ON permissions(module);
