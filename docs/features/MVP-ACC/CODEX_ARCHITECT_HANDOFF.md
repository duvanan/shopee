# MVP-ACC — Codex Architect Handoff
> **Status: READY FOR ARCHITECT / NOT TECH-DESIGN APPROVED** · Ngày 05/11/2026  
> GitHub Issue: https://github.com/duvanan/shopee/issues/43  
> Phạm vi: Account & Authentication — Shopee Clone MVP R0 (Guest, Customer, Admin; Seller out-of-scope)

## 1. Sources of truth (đã duyệt)
- `BA_SPEC.md`: các quyết định nghiệp vụ đã duyệt; 8 tính năng; BR-ACC-01..62, AC-ACC-01..66, EC-ACC-01..34 và BR-CART-001.
- `UX_SPEC.md`: UX approved ngày 05/11/2026 theo xác nhận chủ dự án; FL-01..08, UX-S01..18, UX-ACC-01..40.
- Figma: https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I — 27 desktop + 6 mobile (khung tĩnh, một số mobile chưa vẽ; chưa kết luận pixel QA).
- Project workflow: `AGENTS.md`, `docs/project-management/WORKFLOW.md`, `docs/project-management/SPEC_HANDOFF.md`, `.codex/agents/architect.toml`, Skill `.agents/skills/microservice-design/SKILL.md`.
- Existing implementation should be **inspected**, not assumed compliant: `backend/api-gateway`, `backend/user-service`, `backend/common`, `frontend/src/api/authApi.ts`, `frontend/src/stores/auth.ts`, `frontend/src/router/index.ts`, `frontend/src/views/LoginView.vue`, `frontend/src/views/RegisterView.vue`.

## 2. Scope of this task
**Chỉ lập kỹ thuật**: `docs/features/MVP-ACC/TECHNICAL_DESIGN.md` (Draft/Needs approval). **Không code Backend/Frontend, không migration chạy thật, không thay đổi BA/UX, không merge.** Coordinator giao custom agent `architect` sau khi đọc Issue và tài liệu. Đề xuất kỹ thuật phải được chủ dự án duyệt trước khi implement.

## 3. Business invariants to preserve
1. Email & phone accounts remain independent; no account auto-linking, contact/shipping data never become login identifiers.
2. Email signup: email+password 12–128; pending account valid 7 days; verify by 24h email link; resend cooldown 60s, max 5 **resends**/hour; new link invalidates old. **Verified email owner sets authoritative password after clicking link**; email success requires manual login. Protect against malicious pre-registration and link scanners.
3. Phone: VN `+84` normalization; 6-digit SMS OTP, 5min TTL, max 5 wrong attempts/challenge, cooldown 60s, max 5 **total sends**/hour + 10/day/phone; new OTP invalidates prior; signup creates Customer **only after valid OTP**, then auto-login. Phone login must never auto-register.
4. Sessions: multiple independent devices; logout revokes **current session immediately** at API; disable account invalidates **all sessions immediately** for protected API calls; inactivity 7 days, absolute 30 days. Design credible JWT revocation/fail-safe policy when Redis or Gateway connectivity fails.
5. Admin: provisioned through trusted management; Admin cannot shop as Customer; Guest cannot add to cart; login redirect to product doesn't auto-add.
6. Phone lost/recycled SIM not self-service recovery in demo; before public release this risk must be treated. No self-service account deletion in demo.
7. Anti-enumeration messaging in login/resend/recovery; do not disclose account existence. 401 vs 403 distinct; credential/session/role changes must propagate to Gateway.

## 4. Technical Design required chapters
- Context/scope, current-state diagram and source audit; component ownership/trust boundaries and interactions.
- REST API contract with method/path, auth, request/response/error schema, pagination/trace convention `backend/common`, status mappings for UX (loading/success/error/disabled/retryAfter) and idempotency.
- PostgreSQL entities/constraints/indexes plus Flyway migration/rollback; identity uniqueness under concurrent requests; no cross-service DB writes, flat FK IDs.
- Token lifecycle (verify/reset/OTP/session), secrets & PII handling; Redis key/TTL and persistence strategy; email/SMS delivery abstraction, resilience.
- Sequence/state diagrams: email signup & resume pending, verify click + password setup, phone signup/login, forgot/reset, logout/revoke, account disable, Guest cart redirect, Admin locking.
- Failure/race matrix: parallel submits, retries, partial commit, concurrent OTP, link scanner, expired/replayed token, clock skew, SMS/email downtime, Redis outage/restart, JWT access token valid after revoke, direct backend access/header spoofing, disabling account across nodes.
- Security assessment with clear P0 mitigations; threat model for SIM swap & recycled phone numbers and anti-enumeration. Distinguish demo risk acceptance from public readiness.
- Dependencies, phased implementation breakdown by file/service owner (Backend/Frontend/QA/Security), acceptance traceability to BR/AC/UX IDs, test plan (unit/integration/e2e/security), observability and rollout/rollback, open questions.

## 5. Business/architecture questions requiring explicit escalation
- BA does **not** specify reset-password link TTL; propose a specific choice with tradeoffs for **human approval**.
- BA does **not** fix session-revocation-on-password-reset policy; propose a specific choice with tradeoffs for **human approval**.
- If strict immediate revocation cannot be reliably guaranteed across microservices under Redis outages, document an architecture with fail-safe protected API behavior and conditions; do **not** silently weaken BR-ACC-50/54.
- Registration initial password plus setting authoritative password after verification is a deliberate approved anti-takeover constraint; do not remove it without BA approval.
- Any API response that helps enumeration should be reviewed; do not expose whether unknown phone is registered via distinguishable error details.

## 6. Done means
- A **Draft** Technical Design document exists in a feature branch and cites these approved specs and Issue #43.
- P0 risks resolved or explicitly blocked, no unapproved policy assumptions.
- Every changed flow mapped to BR/AC/UX IDs and planned tests.
- Hand-off reviewed by **user** before Backend/Frontend implementation. No PR merge and no 'Done' status before real evidence.

## 7. Codex Coordinator prompt
```text
MVP-ACC / Issue #43. Read AGENTS.md, docs/project-management/WORKFLOW.md,
SPEC_HANDOFF.md, docs/features/MVP-ACC/BA_SPEC.md, UX_SPEC.md,
CODEX_ARCHITECT_HANDOFF.md and .agents/skills/microservice-design/SKILL.md.
These BA/UX specs are user-approved; do not redo BA/UX discovery.
Delegate the ARCHITECT role only. Create TECHNICAL_DESIGN.md as Draft,
document API/database/session/OTP/Gateway/trust-boundary/data ownership,
P0 risk mitigations, tests, dependencies and unresolved decisions.
Do not implement Backend/Frontend, do not change approved BA/UX decisions,
do not merge PR; return the Technical Design for my review.
```
