-- status: khoa/mo tai khoan THU CONG boi admin (khac locked_until - tu dong
-- khoa tam khi dang nhap sai nhieu lan, xem V5). Chi ACTIVE/LOCKED o phase
-- nay, chua can INACTIVE/DELETED vi chua co tinh nang tuong ung.
ALTER TABLE users ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
CREATE INDEX idx_users_status ON users(status);
