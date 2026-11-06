# MVP-ACC — Phase A Coordinator checkpoint

Issue: [#43](https://github.com/duvanan/shopee/issues/43), Architect-only. Date: 2026-11-06. Status: **Review complete; targeted owner decisions pending; implementation not started.**

This packet records the current conversation's explicit Kafka-first selection. It does not approve the three Technical Designs, change Approved BA/UX, commit sprint capacity, or authorize baseline integration. No original design has been overwritten.

## Preflight evidence

- Read repository workflow, MULTI_AGENT, SPEC_HANDOFF, roadmap, config/role definitions, approved BA/UX and the three technical designs. BA v1.0 and UX v1.0 include owner approval evidence; technical designs remain Draft.
- `git fetch origin main chore/MVP-ACC-approved-specs-architect-handoff` succeeded after filesystem escalation. Starting working tree was clean, on the handoff branch.
- HEAD/handoff: `b490804036d43f3c03f2093c007d5446884b9e54`; main: `8f2e17666e93a2a21d15a10e4d4d08961b9a14c9`; merge base: `fef345d252922f0b36b2687103c1860de03c3d65`. Divergence is **1 / 1**, not an unresolved application-code conflict.
- Main-only commit is merge PR #44; handoff-only commit adds Draft architecture. Direct tip diff: three new design documents plus MVP-ACC README. No merge/rebase/cherry-pick/switch/commit performed.
- GitHub connector verified [#43](https://github.com/duvanan/shopee/issues/43) open and Architect-only; [PR #44](https://github.com/duvanan/shopee/pull/44) merged the earlier BA/UX handoff, not `b490804`. Open-PR search returned no results. No new GitHub Issue/PR created.
- Related open Issues fetched individually: [#24 stale JWT](https://github.com/duvanan/shopee/issues/24), [#27 Order events](https://github.com/duvanan/shopee/issues/27), [#28 Notification](https://github.com/duvanan/shopee/issues/28), [#29 Kafka3](https://github.com/duvanan/shopee/issues/29), [#31 fault/logging tests](https://github.com/duvanan/shopee/issues/31). Search pagination is not proof of a complete repository inventory.
- Host physical RAM: 16,959,066,112 bytes (~15.79 GiB). Docker info failed because the Docker daemon pipe was unavailable. Docker VM allocation, free disk and cluster feasibility remain unverified. Docker was not started or reconfigured.

## Actual delegation and outputs

Runtime successfully ran these three custom roles, respecting the maximum of three simultaneous subagents. No BA/UI-UX discovery agents or implementation agents were started.

| Runtime task / role | Assigned ownership | Result |
|---|---|---|
| `/root/architecture_review` / architect | Read-only design/source review, then sole ownership of new architecture review report | Three designs structurally consistent; all 30 decision IDs mapped; six contract gaps identified |
| `/root/delivery_plan` / planner | Read-only planning/GitHub review, then sole ownership of new issue-draft report | Independent slices, dependency/ownership plan, estimate discrepancy and capacity conflict |
| `/root/security_review` / security | Independent read-only threat/source review, then sole ownership of new security report | Three P0 and three P1 source findings; security acceptance gates; no runtime exploit claim |

Coordinator owns only this new checkpoint document. Agents were instructed not to change original designs/code, overwrite other work, stage/commit, mutate GitHub or run real providers.

Reports:

- [Architecture review and all 30 decisions](PHASE_A_ARCHITECTURE_REVIEW.md)
- [Prepared implementation Issue drafts](PHASE_A_ISSUE_DRAFTS.md)
- [Independent security review](PHASE_A_SECURITY_REVIEW.md)

Skills actually applied: Architect `microservice-design`; Security `security-review` and `testing-strategy`. Skills alone are not approval or passing test evidence.

## Decisions already selected by the owner

Evidence is the explicit instruction in the current conversation, dated 2026-11-06:

- User owns Account/Auth/OTP/tokens/session/business quotas; User PostgreSQL transactional outbox publishes to Kafka3 KRaft; Notification owns its separate PostgreSQL/inbox/delivery workers/templates/provider adapters/retry/DLQ.
- Gateway validates JWT, live sessions and trusted workload identity. Redis holds opaque rotating refresh tokens. Only User verifies OTP, activates accounts and issues sessions.
- Notification requests User JIT authorization. Frontend 202 follows User PG commit, independent of Kafka/Notification/provider ACK.
- Kafka-first is the sole primary delivery path. No HTTP direct-send/Hybrid primary delivery or automatic synchronous fallback. Failure of a performance gate requires capacity/design review, not silently enabling the Draft's synchronous alternative.
- Order/Payment/Shipping/Push/In-App delivery is outside this implementation. No real provider/KMS/secret usage until approved/configured; fake adapters and synthetic fixtures are permitted.

These settle portions of TD-D03/10/11/12, N-D01/05/06/10 and KN-D06/08. They do not approve every field or parameter within those rows.

## Targeted approval register

All recommendations below are **pending owner decision**, including numerical defaults. Approval may be per row/slice; unrelated reviews and contract preparation need not stop.

| Group / decision IDs | Concrete recommendation for review | Scope blocked until resolved |
|---|---|---|
| P1 Reset — TD-D01/02 | Reset link 30m, one use; reset atomically increases epoch/revokes every session, no auto-login | Reset behavior/API/UI/tests |
| P2 Session/trust — remaining TD-D03/04, N-D06 | Primary PG live check every protected request, no positive cache; mTLS peer allowlists. Revoke effective for checks starting after revoke commit; previously authorized work may finish. Approve operational availability/load tradeoff and PKI ownership | Session/Gateway trust integration and acceptance |
| P3 Migration — TD-D05 | Audit first; verified only with evidence; unclear legacy identities require verification/reset; mixed Admin/Customer manual disposition; no automatic merging/deleting UUIDs. Forced legacy re-login and maintenance/cutover need separate approval | Backfill/cutover; audit tooling can be prepared |
| P4 Quota/abuse — TD-D06/08 | Rolling 1h/24h; failed/UNKNOWN reservations still count; email initial excluded from resend count, re-register counts as resend; phone signup/login share quota. Decide current email lock 5 wrong/15m, proposed 20 auth requests/min/IP, global cost cap and audit retention separately | Exact quota/abuse implementation; public trial caps |
| P5 Defaults — TD-D09 | JWT 5m; password 12–128 Unicode code points, no truncation/normalization; benchmark versioned Argon2id; profile trimmed/nonblank max255; new reset invalidates older links; idempotency receipt24h | Password/profile/shared API defaults |
| P6 Local keys — N-D02, remaining TD-D11 | Select local keystore/key-manager mechanism with keys outside Git and separate signing/KEK/HMAC/JWT purposes; AES256-GCM+Ed25519 byte fixtures; real KMS deployment and access/rotation/backup policy approved separately | Crypto implementation; no need to select real provider to prepare contract fixtures |
| P7 JIT/latency — remaining TD-D11/12, N-D04/09, KN-D07 | Grant max5s, expiry runway30s; fake-provider workload10 eligible events/s for10m, two Notification instances; commit→provider ACK p95≤5s/p99≤15s. Accept that pre-resend granted/in-flight stale messages may arrive, but old generation never verifies. Poll250ms/batch20/lease30s, provider timeout3s, max3 safe attempts are provisional tuning; exact backpressure/hysteresis thresholds must be specified | JIT/worker/load acceptance; no synchronous fallback authorized |
| P8 Retention/replay — N-D08, KN-D03, remaining TD-D11 | Auth topic24h, sanitized retry/DLQ signals7d, inbox/dedupe/audit metadata30d, outbox metadata7d; wipe encrypted auth body terminal/expired, outbox body after ACK. Approve grant/UNKNOWN/key/backup retention and max replay horizon together; dedupe must cover every allowed replay | Cleanup/replay/DR; no exact physical/crypto deletion promise |
| P9 Kafka topology/resources — KN-D01/02/04/05 | Local-only three combined broker/controllers, static quorum; compatible pinned patch/digest; auth topic6 partitions, RF3/minISR2/acks=all; all listeners mTLS/default-deny ACL; separate volumes. Hypothesis 2GiB/container and1GiB heap, VM12GiB/free disk20GiB must be measured | Secure cluster provisioning/startup; same host is not production HA |
| P10 Delivery mechanics/templates — N-D03/05/06/07/09, KN-D06 | Polling relay, inbox-before-offset, DB retry scheduler, signal-only DLQ; pinned vi-VN templates and review owner; UNKNOWN never blindly resent or auto-failed-over. Freeze exact retryable evidence/error/outcome schemas before dispatch | Notification application; fake adapters allowed, real SMTP/SMS vendor/budget/sender/support TD-D07 remain public/live-only gate |
| P11 Capacity/DR — remaining TD-D10, N-D10, KN-D08 | Prioritize/auth allocate work explicitly against Cart→Order→Payment, without changing dates or32h/sprint automatically. Accept local single-host limits; specify RPO/RTO and restore stop/reconcile procedure before operational sign-off. Resolve overlap #24/#28/#29/#31 without silently changing existing Issues | Sprint commitment, rollout/DR acceptance |
| P12 Reviewed baseline | Prepare separate review PR for `b490804` plus review packet; owner reviews/integrates via repository workflow. Feature branches then start from reviewed integrated main. No automatic merge/rebase/cherry-pick | Implementation branch baseline |

Owner can approve a subset, e.g. local config-only Kafka foundation after P9/P11/P12, while reset/provider policies remain pending. No slice is currently marked Ready.

## Technical reconciliation and remaining contract work

No BA/UX contradiction requires reopening discovery. Coordinator accepts the following engineering closure work from Architect, not a new business policy:

1. Freeze machine-readable event schema and JIT/OpenAPI success/error/outcome/reconciliation enums; existing examples are illustrative, not executable interoperability fixtures. Domain denial HTTP mapping must have one canonical definition.
2. Freeze exact canonical AAD, wrapping mechanism, key-ID constraints, binary lengths, digest/signature byte fixtures and negative cases. Keep the original immutable signed envelope for digest checks; never reconstruct it from mutable delivery state.
3. Define one lock-order table spanning identity rate row, workflow, account, token/challenge and session; prove no inverted order across resend/verify/cleanup/disable.
4. Differentiate definitely-not-dispatched vs MAY_HAVE_DISPATCHED recovery. Extra phases help only if a durable dispatch transition is mandatory before every provider I/O, stale workers cannot pass it and SDK hidden retries are disabled. Otherwise retain UNKNOWN and suppress blind resend. No DB phase fences an external provider socket.
5. Same producer/idempotencyKey with a different event or digest is a conflict/quarantine, not a second logical send or silent coalescing; test unique-index races.
6. Unify retention/replay horizons across User outbox/grants, Kafka, Notification receipts, providers/keys/backups. Expiry and JIT remain mandatory even when dedupe metadata has been purged.

These can be completed as Draft design work under #43 while owner decisions are pending. Production implementation must use separate Issues.

## Execution and ownership plan

Prepared slices are detailed in the Issue-draft report; provisional IDs are not GitHub numbers. All remain Backlog/Unscheduled with target date and sprint pending capacity approval.

1. Reviewed baseline + scoped decisions → contract freeze + Security review.
2. Parallel independent paths: DevOps secure Kafka/config/DB provisioning; Backend User audit/schema/session/quota; Backend Notification module/inbox after contracts. Maximum3 subagents; no shared-file editing.
3. Integrate User outbox publisher/JIT with encrypted ingress/inbox; fake adapters/templates then durable dispatcher/UNKNOWN handling. No provider send without persisted attempt and valid JIT.
4. User email/phone/reset/profile/disable flow slices. Frontend may prepare types/fixtures/UI after API freeze; endpoint integration follows Backend. Email verified→Login, phone signup valid OTP→create+login, 202 never means delivered.
5. QA and Security independently review every changed slice; fixes return to assigned implementer. Real Kafka3/PG/Redis fault tests plus app crash/replay/lag/UNKNOWN/leakage tests, cross-node revocation, then responsive/accessibility/E2E.
6. Cutover/rollback rehearsal and owner review. No mixed JWT-only fleet, no dual delivery owner, no destructive schema/volume rollback.

Shared file integration: Notification module owner alone edits `backend/pom.xml`; contract owner alone edits `backend/common`; DevOps alone edits root Compose/DB bootstrap/CI; User AuthService/AuthController/migration numbering and Frontend store/http/router change sequentially. Independent review is read-only.

First candidate after approvals: K1 config-only topology/listener/persistence slice6–8h, startup disabled until K2 mTLS/default-deny ACL gates pass. Alternatively a separately scoped read-only legacy audit tool can precede backfill approval. Neither candidate authorizes all Auth+Kafka+Notification in one PR.

## Estimate and roadmap risk

A1..A11 sum to **86–126h**, not the stated90–130h. Notification increments48–72h and Kafka24–36h produce **158–234h itemized**, versus **162–238h headline**. The unexplained4h is not silently converted to contingency or removed; Architect/Planner must reconcile its actual ownership before commitment.

Headline162–238h =5.06–7.44 sprints at32 committed hours, or10.1–14.9 weeks at16 committed hours/week. S1–S5 contain160 committed hours for the entire existing MVP plan; the new program alone exceeds that by2–78h. S1–S8 contain256h; new program consumes63–93% before other work. No deadline guarantee or new sprint allocation is defensible yet. Agent concurrency does not increase the owner's20h/week capacity.

#29 currently depends on Order producer #27 and #28 explicitly covers Order events/email-via-log; neither is suitable unchanged for Auth-first. Preserve both and link proposed Auth slices; owner must decide rescope/split/overlap accounting. ELK and Order producers remain outside this scope.

## Validation and prepared PR

Executed: document/source reads, `rg`, Git status/log/diff/merge-base/rev-list, successful fetch, GitHub issue/PR reads, resource probe. Starting `git diff --check` passed for tracked changes; new report checks are recorded in Coordinator's final response.

**Not run:** Maven/npm builds, migrations, cluster startup, integration/E2E/fault/performance/exploit tests. This is a review/planning checkpoint; no application behavior changed and no real messages sent. Docker resource probe failure is not a cluster test result.

Prepared design-review PR title: `docs(MVP-ACC): Kafka-first architecture review and scoped delivery gates (#43)`.

Prepared body: Refs #43. Review Auth v0.2, Notification and Kafka3 Draft designs plus Phase A reports. Owner selected Kafka-first boundaries; remaining decision IDs are explicitly pending. Include contract closure list, provisional issue drafts, capacity discrepancy and P0/P1 source gates. BA/UX unchanged. Validation is static only; runtime builds/fault tests Not Run. No migration or rollout. Request owner review and baseline integration; do not merge/close #43 or mark whole Technical Design Approved.

There is no new Issue/PR URL because nothing has been published. This review packet and issue drafts are the concrete preparation for the owner's decision checkpoint.
