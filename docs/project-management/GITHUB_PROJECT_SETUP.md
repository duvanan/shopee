# Thiết lập GitHub Projects Kanban cho Shopee Clone

> GitHub Issues đã được lập trực tiếp. **GitHub Projects board phải được tạo trên giao diện GitHub** vì phiên kết nối hiện tại không hỗ trợ API tạo/chỉnh Project; file này là hướng dẫn, không được hiểu là board đã tồn tại.

1. Mở https://github.com/users/duvanan/projects → **New project** → **Board**. Đặt tên: `Shopee Clone Delivery 2026–2027`.
2. Thêm repo `duvanan/shopee` làm source và thêm các Issue có prefix `[S1]`…`[S8]`.
3. Chỉnh **Status**: `Backlog`, `Ready`, `In Progress`, `In Review`, `Testing`, `Done`; thêm `Blocked` riêng nếu muốn.
4. Tạo custom fields: `Sprint` (S1–S8), `Priority` (P0/P1/P2), `Estimate (h)` (number), `Actual (h)` (number), `Target date` (date), `Type` (Feature/Bug/Chore/Spike).
5. Tạo 3 views: **Board by Status**; **Current Sprint** (filter Sprint); **Deadline** (table sort Target date).
6. Dùng Issues `[SPRINT S1]`…`[SPRINT S8]` làm checklist tổng. Tạo GitHub Milestones theo ngày kết thúc sprint nếu muốn tích hợp due date cho Issue.
7. Automation có thể bật qua menu Workflows: add item → Backlog, PR merged → Done (chỉ nếu CI/test và AC đã xác nhận). **Không auto Done chỉ vì PR merged** nếu còn testing.

## Gắn Issues và PR
- Issues có mã `CART-03`, có AC và DoD.
- Branch: `feature/CART-03-update-cart`.
- PR title: `feat(cart): update quantity (CART-03)`.
- PR body: `Closes #<issue-number>` (chỉ khi thật sự Done).

**Lưu ý:** Project custom fields không tự đồng bộ vào file Excel. Cần cập nhật hàng tuần hoặc thiết lập automation riêng sau này.
