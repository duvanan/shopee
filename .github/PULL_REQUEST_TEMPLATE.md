## Issue liên quan
- Closes #<!-- number; chỉ dùng khi thỏa DoD -->
- Feature ID: `...`

## Thay đổi
- ...

## Scope / Non-goals
- ...

## Bằng chứng kiểm thử
- [ ] `mvn -B -ntp verify -f backend/pom.xml` (ghi kết quả nếu không chạy)
- [ ] `cd frontend && npm ci && npm run build` (ghi kết quả nếu không chạy)
- [ ] Unit/Integration test có case thành công và thất bại
- [ ] Manual QA / E2E (mô tả bước + expected/actual)
- [ ] Security test khi chạm auth/role/ownership

## Review checklist
- [ ] AC của Issue đã được kiểm chứng
- [ ] Không secret/credential trong diff
- [ ] API contract và response lỗi tương thích
- [ ] DB migration/versioning an toàn (nếu có)
- [ ] Không sửa code ngoài scope; docs cập nhật nếu cần
- [ ] CI xanh và không còn blocker nghiêm trọng

## Notes
- Risk/rollback plan (nếu cần):
- Screenshots (khi có frontend):
