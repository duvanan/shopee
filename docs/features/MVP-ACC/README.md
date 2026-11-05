# MVP-ACC — Account & Authentication (MVP R0)

| Artifact | Status | Authority |
|---|---|---|
| [BA_SPEC.md](BA_SPEC.md) | **Approved** business rules v1.0 | Chủ dự án; 29 lựa chọn A + quyết định đã chốt |
| [UX_SPEC.md](UX_SPEC.md) | **Approved** UX content v1.0 (05/11/2026) | Chủ dự án xác nhận UX ổn và yêu cầu bước tiếp theo |
| [CODEX_ARCHITECT_HANDOFF.md](CODEX_ARCHITECT_HANDOFF.md) | Ready for Architect review | Checklist phục vụ [Issue #43](https://github.com/duvanan/shopee/issues/43) |
| [TECHNICAL_DESIGN.md](TECHNICAL_DESIGN.md) | **Draft v0.2 / Needs review — chưa phê duyệt** | Auth + shared notification/Kafka integration; TD-D01..12 chờ review |
| [NOTIFICATION_SERVICE_TECHNICAL_DESIGN.md](../../architecture/NOTIFICATION_SERVICE_TECHNICAL_DESIGN.md) | **Draft / Needs review** | Shared service, events, delivery/JIT/retry và N-D01..10 |
| [KAFKA_NOTIFICATION_ARCHITECTURE.md](../../architecture/KAFKA_NOTIFICATION_ARCHITECTURE.md) | **Draft / Needs review** | Kafka3 KRaft, topology/topics/ACL/recovery và KN decisions |

Figma: https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I  
Nhánh bàn giao: `chore/MVP-ACC-approved-specs-architect-handoff`

**Bản sửa thiết kế 05/11/2026:** Theo yêu cầu trực tiếp của chủ dự án, draft mới đưa Notification Service dùng chung và Kafka3 vào thiết kế Auth. User vẫn sở hữu OTP/token/session/business send quota; Notification sở hữu templates/provider/delivery và DB riêng. Không thay approved BA/UX. Handoff cũ là lịch sử đầu vào; phần giới hạn một artifact/direct sender được thay bởi phạm vi design mới trong Technical Design v0.2. Chưa có implementation hay thay đổi Compose/migration. Ước lượng162–238h là preliminary, cần quyết định roadmap trước triển khai.

**Lưu ý:** Figma có 27 desktop + 6 mobile; các biến thể mobile còn thiếu và chưa kiểm thử responsive, không cản trở Architect thiết kế contract nhưng phải xử lý trước khi nghiệm thu UX implementation. BA/UX approved **không** đồng nghĩa Technical Design hoặc code đã được phê duyệt. Không tự triển khai Backend/Frontend hay auto-merge.
