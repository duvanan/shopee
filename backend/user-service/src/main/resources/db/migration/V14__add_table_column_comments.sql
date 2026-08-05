-- Chi them COMMENT ON TABLE/COLUMN de tu tai lieu hoa schema ngay trong
-- database (xem qua \d+ ten_bang hoac cong cu ERD/GUI bat ky) - khong doi
-- cau truc/du lieu.

-- ===================== users =====================
COMMENT ON TABLE users IS 'Tai khoan nguoi dung (khach hang + nhan vien/admin dung chung 1 bang) - phan quyen qua bang roles/permissions rieng (xem user_roles), khong co cot role/is_admin tren bang nay.';
COMMENT ON COLUMN users.email IS 'Dang nhap bang email, duy nhat, khong phan biet hoa/thuong o tang service (chuan hoa lowercase truoc khi luu/so sanh).';
COMMENT ON COLUMN users.password_hash IS 'Mat khau da bam (BCrypt) - KHONG BAO GIO luu/tra ve plaintext.';
COMMENT ON COLUMN users.full_name IS 'Ho ten hien thi - khong bat buoc luc dang ky, co the bo trong.';
COMMENT ON COLUMN users.phone IS 'So dien thoai lien he - khong bat buoc, khong dung de dang nhap.';
COMMENT ON COLUMN users.created_by IS 'ID nguoi tao tai khoan nay - NULL neu tu dang ky, co gia tri neu admin tao ho qua man Quan ly nguoi dung.';
COMMENT ON COLUMN users.updated_by IS 'ID nguoi sua thong tin tai khoan nay lan gan nhat (co the la chinh user do hoac admin).';
COMMENT ON COLUMN users.failed_login_attempts IS 'Dem so lan dang nhap sai LIEN TIEP gan nhat - reset ve 0 khi dang nhap thanh cong, dung de kich hoat khoa tai khoan tam thoi (xem locked_until).';
COMMENT ON COLUMN users.locked_until IS 'Thoi diem tai khoan duoc TU DONG MO KHOA sau khi bi khoa vi dang nhap sai qua nhieu lan - NULL nghia la khong bi khoa.';
COMMENT ON COLUMN users.last_login_at IS 'Thoi diem dang nhap thanh cong gan nhat - cap nhat moi lan login, khac voi login_histories la log CHI TIET tung lan (ca thanh cong lan that bai).';
COMMENT ON COLUMN users.status IS 'ACTIVE/INACTIVE/... - tai khoan khong o trang thai ACTIVE se bi tu choi dang nhap du dung mat khau (kiem tra truoc ca buoc so sanh mat khau).';
COMMENT ON COLUMN users.password_changed_at IS 'Thoi diem doi mat khau gan nhat (dang ky, quen mat khau, hoac tu doi trong tai khoan) - du phong cho chinh sach bat doi mat khau dinh ky, hien chua co logic enforce.';

-- ===================== roles =====================
COMMENT ON TABLE roles IS 'Vai tro (VD: ADMIN, STAFF, CUSTOMER) - 1 user co the co nhieu role (qua user_roles), moi role gom nhieu permission (qua role_permissions). Day la lop trung gian giup gan quyen theo nhom thay vi gan tung permission le cho tung user.';
COMMENT ON COLUMN roles.code IS 'Ma vai tro, duy nhat, dung trong logic phan quyen va JWT claims (VD: "ADMIN") - khong doi sau khi tao vi co the da duoc hardcode o Gateway/FE.';
COMMENT ON COLUMN roles.name IS 'Ten hien thi cua vai tro cho admin xem (VD: "Quan tri vien") - khac code, co the sua tu do.';
COMMENT ON COLUMN roles.is_system IS 'true = vai tro he thong (VD ADMIN/CUSTOMER mac dinh tao san luc migrate) - khong cho xoa qua UI de tranh vo he thong phan quyen, chi role tuy bien (is_system=false) moi xoa duoc.';

-- ===================== permissions =====================
COMMENT ON TABLE permissions IS 'Danh sach quyen han cu the (VD: PRODUCT_CREATE, USER_MANAGE) - don vi nho nhat cua he thong phan quyen, duoc gan cho role qua role_permissions. API Gateway doc permission cua user (embed trong JWT/header) de quyet dinh cho phep goi endpoint hay khong (xem config-repo/api-gateway.yml).';
COMMENT ON COLUMN permissions.code IS 'Ma quyen, duy nhat, dung truc tiep trong cau hinh Gateway va @PreAuthorize/check o cac service (VD: "PRODUCT_CREATE") - khong doi sau khi tao.';
COMMENT ON COLUMN permissions.name IS 'Ten hien thi cua quyen cho admin xem (VD: "Tao san pham").';
COMMENT ON COLUMN permissions.description IS 'Mo ta chi tiet pham vi/y nghia cua quyen, ho tro admin khi gan quyen cho role.';
COMMENT ON COLUMN permissions.is_system IS 'true = quyen he thong, duoc seed san qua migration, khong cho xoa qua UI (vi co the dang duoc Gateway/service tham chieu cung).';
COMMENT ON COLUMN permissions.module IS 'Nhom chuc nang cua quyen (VD: "PRODUCT", "USER", "AUDIT_LOG") - phuc vu gom nhom hien thi tren man Phan quyen, khong anh huong logic check quyen.';

-- ===================== user_roles =====================
COMMENT ON TABLE user_roles IS 'Bang lien ket nhieu-nhieu giua users va roles - 1 user co the duoc gan nhieu role cung luc (VD vua STAFF vua co role tuy bien khac).';
COMMENT ON COLUMN user_roles.user_id IS 'FK toi users.id.';
COMMENT ON COLUMN user_roles.role_id IS 'FK toi roles.id.';
COMMENT ON COLUMN user_roles.assigned_by IS 'FK (mem, khong co constraint) toi users.id cua admin da thuc hien gan role nay - phuc vu audit "ai cap quyen cho ai".';
COMMENT ON COLUMN user_roles.assigned_at IS 'Thoi diem role duoc gan cho user.';

-- ===================== role_permissions =====================
COMMENT ON TABLE role_permissions IS 'Bang lien ket nhieu-nhieu giua roles va permissions - dinh nghia 1 role duoc phep lam nhung gi. Quyen thuc te cua 1 user = HOP cac permission cua TAT CA role ma user do dang co.';
COMMENT ON COLUMN role_permissions.role_id IS 'FK toi roles.id.';
COMMENT ON COLUMN role_permissions.permission_id IS 'FK toi permissions.id.';
COMMENT ON COLUMN role_permissions.assigned_by IS 'FK (mem) toi users.id cua admin da gan permission nay cho role - phuc vu audit.';
COMMENT ON COLUMN role_permissions.assigned_at IS 'Thoi diem permission duoc gan cho role.';

-- ===================== login_histories =====================
COMMENT ON TABLE login_histories IS 'Log CHI TIET tung lan co gang dang nhap/dang xuat/lam moi phien (ca thanh cong lan that bai) - bang append-only, phuc vu audit bao mat va phat hien dang nhap bat thuong. Khac users.last_login_at chi luu lan gan nhat.';
COMMENT ON COLUMN login_histories.user_id IS 'FK (mem) toi users.id - NULL neu la lan dang nhap that bai vi email khong ton tai (khong xac dinh duoc user).';
COMMENT ON COLUMN login_histories.email IS 'Email dung trong lan thu dang nhap - luu lai du user_id la NULL, phuc vu dieu tra brute-force theo email.';
COMMENT ON COLUMN login_histories.action IS 'Loai hanh dong: LOGIN/LOGOUT/REFRESH/... .';
COMMENT ON COLUMN login_histories.status IS 'SUCCESS/FAILURE - ket qua cua hanh dong.';
COMMENT ON COLUMN login_histories.session_id IS 'Dinh danh phien lam viec (khop voi refresh token session luu o Redis) - dung de noi cac su kien LOGIN/REFRESH/LOGOUT cung 1 phien.';
COMMENT ON COLUMN login_histories.ip_address IS 'Dia chi IP nguon cua request.';
COMMENT ON COLUMN login_histories.user_agent IS 'User-Agent trinh duyet/thiet bi thuc hien request - phuc vu nhan dien thiet bi trong man "Phien dang nhap".';
COMMENT ON COLUMN login_histories.failure_reason IS 'Ly do that bai (VD: "Sai mat khau", "Tai khoan bi khoa") - chi co gia tri khi status=FAILURE.';
COMMENT ON COLUMN login_histories.trace_id IS 'Trace ID cua request (lien ket voi log ung dung/APM) de dieu tra su co.';

-- ===================== password_reset_tokens =====================
COMMENT ON TABLE password_reset_tokens IS 'Token dung 1 lan cho luong "quen mat khau" - luu HASH cua token (khong luu token goc) de ngay ca khi lo du lieu DB cung khong the gia mao token.';
COMMENT ON COLUMN password_reset_tokens.user_id IS 'FK toi users.id yeu cau reset mat khau.';
COMMENT ON COLUMN password_reset_tokens.token_hash IS 'Hash (SHA-256) cua token gui qua email - token goc CHI hien thi 1 lan cho user, khong bao gio luu plaintext trong DB.';
COMMENT ON COLUMN password_reset_tokens.expires_at IS 'Thoi diem token het han - token qua han bi tu choi du chua duoc dung.';
COMMENT ON COLUMN password_reset_tokens.used_at IS 'Thoi diem token da duoc dung de doi mat khau thanh cong - NULL nghia la chua dung; token da dung roi thi khong dung lai duoc lan 2 (chong replay).';
COMMENT ON COLUMN password_reset_tokens.status IS 'ACTIVE/USED/EXPIRED/REVOKED - trang thai tong hop de truy van nhanh thay vi tinh toan lai tu expires_at/used_at moi lan.';
COMMENT ON COLUMN password_reset_tokens.requested_ip IS 'Dia chi IP cua request yeu cau reset mat khau - phuc vu audit/phat hien lam dung.';
COMMENT ON COLUMN password_reset_tokens.user_agent IS 'User-Agent cua request yeu cau reset mat khau.';

-- ===================== customer_addresses =====================
COMMENT ON TABLE customer_addresses IS 'So dia chi giao hang cua khach hang - 1 user co the luu nhieu dia chi, chon 1 dia chi lam mac dinh (is_default) de tu dien san luc dat hang.';
COMMENT ON COLUMN customer_addresses.user_id IS 'FK toi users.id chu so huu dia chi.';
COMMENT ON COLUMN customer_addresses.recipient_name IS 'Ten nguoi nhan hang tai dia chi nay - co the khac ten chu tai khoan (VD nhan ho).';
COMMENT ON COLUMN customer_addresses.phone_number IS 'So dien thoai nguoi nhan, dung de don vi van chuyen lien he khi giao hang.';
COMMENT ON COLUMN customer_addresses.province_code IS 'Ma tinh/thanh pho (theo danh muc hanh chinh chuan) - dung kem province_name de tra cuu/doi soat.';
COMMENT ON COLUMN customer_addresses.province_name IS 'Ten tinh/thanh pho hien thi - luu san (denormalize) de khong phai join bang danh muc hanh chinh moi lan hien thi.';
COMMENT ON COLUMN customer_addresses.district_code IS 'Ma quan/huyen.';
COMMENT ON COLUMN customer_addresses.district_name IS 'Ten quan/huyen hien thi - luu san tuong tu province_name.';
COMMENT ON COLUMN customer_addresses.ward_code IS 'Ma phuong/xa.';
COMMENT ON COLUMN customer_addresses.ward_name IS 'Ten phuong/xa hien thi - luu san tuong tu province_name.';
COMMENT ON COLUMN customer_addresses.address_line IS 'Dia chi chi tiet (so nha, ten duong...) sau cap phuong/xa.';
COMMENT ON COLUMN customer_addresses.address_type IS 'Phan loai dia chi: HOME (nha rieng)/OFFICE (co quan)/... - chi de hien thi/loc, khong anh huong logic giao hang.';
COMMENT ON COLUMN customer_addresses.label IS 'Nhan tuy chinh cho dia chi (VD: "Nha ba ngoai") - khong bat buoc.';
COMMENT ON COLUMN customer_addresses.is_default IS 'true = dia chi mac dinh, tu dien san khi tao don hang moi - moi user nen co toi da 1 dia chi is_default=true (enforce o tang service).';

-- ===================== audit_logs =====================
COMMENT ON TABLE audit_logs IS 'Log audit CHUNG cho cac hanh dong quan tri quan trong (tao/sua/xoa user, gan role, doi permission...) - bang append-only, khac login_histories chi tap trung vao dang nhap/dang xuat. Dung de tra loi "ai da lam gi, luc nao, thay doi gi".';
COMMENT ON COLUMN audit_logs.actor_id IS 'FK (mem) toi users.id cua nguoi thuc hien hanh dong - NULL neu hanh dong duoc thuc hien tu dong boi he thong (khong co actor la nguoi dung).';
COMMENT ON COLUMN audit_logs.actor_email IS 'Email cua nguoi thuc hien tai thoi diem ghi log - luu san (denormalize) de van doc duoc du sau nay user bi xoa hoac doi email.';
COMMENT ON COLUMN audit_logs.actor_type IS 'Loai actor: USER (nguoi dung qua UI) / SYSTEM (tien trinh tu dong) / ... .';
COMMENT ON COLUMN audit_logs.action IS 'Loai hanh dong (VD: CREATE/UPDATE/DELETE/ASSIGN_ROLE...).';
COMMENT ON COLUMN audit_logs.module IS 'Module nghiep vu lien quan (VD: "USER", "ROLE", "PRODUCT") - phuc vu loc log theo khu vuc chuc nang.';
COMMENT ON COLUMN audit_logs.object_type IS 'Loai doi tuong bi tac dong (VD: "User", "Role") - dung kem object_id de xac dinh chinh xac ban ghi nao bi thay doi.';
COMMENT ON COLUMN audit_logs.object_id IS 'ID cua doi tuong bi tac dong - kieu chuoi (khong phai UUID cung) vi co the tro toi doi tuong o service/database khac, khong dat FK constraint.';
COMMENT ON COLUMN audit_logs.description IS 'Mo ta ngan gon, de doc cho hanh dong (VD: "Gan role ADMIN cho user X") - hien thi truc tiep tren man Nhat ky, khong can dich lai tu action/module.';
COMMENT ON COLUMN audit_logs.old_value IS 'JSON snapshot gia tri TRUOC khi thay doi - phuc vu xem lai lich su/rollback thu cong, khong tu dong dung de rollback.';
COMMENT ON COLUMN audit_logs.new_value IS 'JSON snapshot gia tri SAU khi thay doi.';
COMMENT ON COLUMN audit_logs.trace_id IS 'Trace ID cua request lien quan (lien ket voi log ung dung/APM) de dieu tra su co.';
COMMENT ON COLUMN audit_logs.status IS 'SUCCESS/FAILURE - ket qua cua hanh dong duoc ghi log (kem error_message neu that bai).';
COMMENT ON COLUMN audit_logs.error_message IS 'Thong diep loi neu hanh dong that bai - chi co gia tri khi status=FAILURE.';
