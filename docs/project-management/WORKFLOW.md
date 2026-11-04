# Shopee Clone — Development Workflow

> Áp dụng từ 09/11/2026. Repo: https://github.com/duvanan/shopee. Giả định: 1 developer, 20 giờ/tuần; mỗi sprint 2 tuần, 32 giờ committed + 8 giờ buffer. Đây là kế hoạch, không phải trạng thái đã hoàn thành.

## Nguồn dữ liệu chuẩn
1. **GitHub Issues**: một task = một Issue; phải có mã tính năng, estimate, sprint, target date, Acceptance Criteria (AC).
2. **GitHub Projects (Kanban)**: nguồn chuẩn về **trạng thái** sau khi người dùng tạo Project và thêm Issues.
3. **Pull Requests + Actions**: bằng chứng code/test; không dùng việc “đã viết code” để kết luận Done.
4. **Excel Feature Roadmap**: báo cáo/backup cập nhật hàng tuần, không tự đồng bộ với GitHub.

## Pipeline
Backlog → Ready → In Progress → In Review → Testing → Done.
- **Backlog**: chưa được commit sprint hoặc thiếu thiết kế.
- **Ready**: có mục tiêu, phạm vi, contract, dependencies, estimate, AC.
- **In Progress**: đang có owner và branch theo Issue.
- **In Review**: PR mở, có mô tả test và kiểm tra security.
- **Testing**: build/CI pass; kiểm thử nghiệp vụ, ngoại lệ, tích hợp.
- **Done**: mọi AC và DoD đạt, PR merge vào main, issue đóng.
- **Blocked**: dùng trường/nhãn riêng trong Project; ghi lý do, next action và ngày kiểm tra lại; báo rủi ro nếu quá 1 ngày làm việc.

## Product discovery and approved spec handoff (business-facing features)
**User + ChatGPT**, not Codex by default, define business and UI/UX requirements:

1. For Catalog, Cart, Inventory, Order, Checkout, Payment, Review and other commerce features: create `docs/features/<FEATURE-ID>/BA_SPEC.md` using `docs/project-management/templates/BA_SPEC.md` and `ecommerce-domain`.
2. If customer/admin screens change: create `docs/features/<FEATURE-ID>/UX_SPEC.md` using `docs/project-management/templates/UX_SPEC.md` and `ui-ux-design`. Backend-only work can mark UX Spec N/A.
3. **The user explicitly approves** required BA and UX Specs, with approval status/reference in those files. A template, Draft, or AI response alone is not approval.
4. **Codex Coordinator** reads approved specs (or links to an approved parent-feature spec) and Issue before technical design/implementation. Architect, Backend, Frontend, QA and Security must comply with BR/AC/UX criteria. Conflicts or blocking missing decisions go back to the user; don't invent policies.
5. Codex `business-analyst` and `ui-ux` roles are **optional on explicit user request** for read-only gap/compliance reviews, not routine discovery.
6. Pure infrastructure, CI, logging, non-business security audits or urgent security fixes may use Issue AC without BA/UX documents. See `SPEC_HANDOFF.md` for details.

## Mỗi task: Definition of Ready (DoR)
- [ ] Phạm vi rõ, có Non-goals.
- [ ] Nếu là feature nghiệp vụ: có BA Spec **được người dùng duyệt** (Status: Approved + approval evidence), hoặc link đến parent-feature spec đã duyệt; không còn business decisions blocking.
- [ ] Nếu thay đổi giao diện: có UX Spec **được người dùng duyệt** (flow, states, responsive, accessibility, UX criteria); nếu backend-only thì UX N/A.
- [ ] Có Acceptance Criteria kiểm chứng được.
- [ ] API contract / DB / Redis/Kafka message nếu liên quan.
- [ ] Dependency được gắn hoặc liệt kê; không vượt quá 8–10 giờ nếu có thể chia nhỏ.
- [ ] Estimate, sprint, target date hợp lệ.

## Phát triển theo GitHub Flow
1. Chọn Issue ở Ready, kiểm tra prerequisite và capacity sprint.
2. Codex xác minh **user-approved BA Spec + UX Spec (nếu có UI)** hoặc approved parent-feature spec; không tự gọi BA/UI-UX làm discovery lại. Sau đó Architect hoàn thiện Technical Design/API handoff. Nếu thiếu/conflict spec cần thiết, dừng phần implementation liên quan và hỏi người dùng.
3. Tạo branch `feature/<ISSUE-ID>-slug`, `fix/<ISSUE-ID>-slug` hoặc `chore/<ISSUE-ID>-slug` từ `main`.
4. Implement theo từng lát nhỏ; backend + frontend + tests cùng một AC.
5. Chạy `mvn -B -ntp verify -f backend/pom.xml` và `cd frontend && npm ci && npm run build`.
6. Tạo PR, link `Closes #<issue-number>` chỉ khi DoD thật sự đạt.
7. Self-review (hoặc nhờ reviewer/Codex review), giải quyết comment, CI pass.
8. Test integration/negative paths; merge `main`; đóng Issue; cập nhật Project/Excel.

**Không merge khi chưa chạy test. Không cho Codex tự quyết định Done/merge mà không kiểm chứng.**

## Definition of Done (DoD)
- [ ] Hành vi đúng API contract, user-approved BA Business Rules/AC và UX acceptance criteria (nếu có giao diện).
- [ ] Tests tự động phù hợp chạy pass; bao gồm lỗi và security của phần sửa.
- [ ] Build backend/frontend pass.
- [ ] Database migrations có tính tương thích/rollback note nếu thay schema.
- [ ] Không commit secret; kiểm soát authentication/authorization và isolation user.
- [ ] Có bằng chứng integration/E2E hoặc ghi rõ phần chưa test và quyết định chấp nhận rủi ro.
- [ ] PR review xong, merge vào `main`, docs cập nhật, Issue đóng.

## Nhịp làm việc
- **Thứ Hai đầu sprint (30–45 phút)**: chọn issue từ Ready, không vượt 32 giờ planned.
- **Hằng ngày (5 phút)**: cập nhật trạng thái task, blocker, actual hours.
- **Thứ Tư (15 phút)**: kiểm tra deadline, scope creep, dependencies.
- **Chủ nhật (30 phút)**: demo, nghiệm thu, đối chiếu actual vs plan; ghi báo cáo theo `WEEKLY_REVIEW.md`.
- **Cuối sprint**: phân loại Done/Carry-over/Cancelled; lập kế hoạch lại task trễ, không đánh dấu Done giả.

## Kiểm soát deadline
- `Planned hours` = tổng estimate task sprint. `Actual hours` = thời gian thực tế đã làm.
- `Completion by scope` = số issue Done / tổng issue committed, nhưng **không** thay thế đánh giá AC.
- Nếu có nguy cơ trễ: (1) xác định blocker; (2) giảm phạm vi non-essential; (3) dời task phụ sang sprint sau; (4) cập nhật deadline mới minh bạch.
- Deadline MVP: 17/01/2027. Deadline Beta: 28/02/2027. Các estimate có thể thay đổi sau sprint 1.
- Không nhận thêm tính năng vào sprint nếu tổng estimate >32h, trừ khi chính thức thay scope.

## Lưu ý với Shopee Clone
- Gateway hiện giải mã JWT và kiểm tra permission từ claims: phải test revoke/permission change/token cũ.
- Microservices truyền X-User-* headers: kiểm tra trust boundary; không expose các backend service trực tiếp ra mạng không tin cậy.
- Redis refresh-token khác Redis cart; Kafka/ELK ở giai đoạn Beta, không chặn MVP.
- Hoàn thành `Cart → Order → Inventory → Payment` trước Search/Seller/Voucher.

Xem `ROADMAP_2026_2027.md`, `FEATURE_BACKLOG.md` và `GITHUB_PROJECT_SETUP.md`.
