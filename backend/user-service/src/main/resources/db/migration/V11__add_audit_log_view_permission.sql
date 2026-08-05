-- AUDIT_LOG_VIEW: permission rieng cho man xem audit log (khac USER_MANAGE/
-- ROLE_MANAGE) - xem docs/architecture/user-auth-role-permission-redis-refresh-token-design-uuid.md muc 19.
INSERT INTO permissions (code, name, description, is_system) VALUES
    ('AUDIT_LOG_VIEW', 'Xem audit log', 'Xem lại nhật ký thao tác nhạy cảm', TRUE);

INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'ADMIN'), id FROM permissions WHERE code = 'AUDIT_LOG_VIEW';
