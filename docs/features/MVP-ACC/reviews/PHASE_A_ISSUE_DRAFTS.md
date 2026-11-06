# MVP-ACC — Phase A implementation Issue drafts

Review/planning parent: [Architect-only Issue #43](https://github.com/duvanan/shopee/issues/43). Prepared by the actual `planner` subagent for Coordinator review on 2026-11-06. This document does not authorize implementation under #43.

**Status of every draft below: Provisional / Backlog / Unscheduled / Not Ready.** IDs are local planning aliases, not GitHub Issue numbers. Sprint, target date, named assignee and production activation remain owner-pending. No GitHub Issue or PR was created by this planning task. BA/UX v1.0 remain Approved; Technical Design remains Draft. The user's Kafka-first selection approves the delivery direction, not every pending policy/default.

## Sources and baseline

Read the repository workflow, MULTI_AGENT, SPEC_HANDOFF, ROADMAP_2026_2027, FEATURE_BACKLOG, planner role, approved BA/UX, and Auth/Notification/Kafka draft designs. Estimate source: [Auth Technical Design §10](../TECHNICAL_DESIGN.md), [Notification §12](../../../architecture/NOTIFICATION_SERVICE_TECHNICAL_DESIGN.md), [Kafka §11](../../../architecture/KAFKA_NOTIFICATION_ARCHITECTURE.md).

Coordinator confirmed fresh fetch: handoff HEAD `b490804`, main `8f2e176`, merge-base `fef345d`, divergence 1/1. PR [#44](https://github.com/duvanan/shopee/pull/44) merged approved BA/UX handoff; subsequent three-design commit is not integrated. No open PR was found by the Coordinator's connector search. No merge, rebase, cherry-pick, overwrite, staging or commit is performed here. Implementation branches must start from a reviewed, legitimately integrated baseline.

GitHub issue search was read-only. Relevant existing scopes:

| Existing Issue | Planning treatment |
|---|---|
| [#43 Architect](https://github.com/duvanan/shopee/issues/43) | Design/review only; no implementation code. |
| [#29 OPS-01](https://github.com/duvanan/shopee/issues/29) | Kafka3 overlap; existing dependency on #27 Order producer is unsuitable for Auth-first. Preserve existing issue pending owner rescope/split decision; link new scoped drafts rather than silently reprioritize. |
| [#28 NOTIFY-01](https://github.com/duvanan/shopee/issues/28) | Existing OrderCreated/OrderPaid and email-via-log scope is not Auth Notification acceptance. Preserve; do not log Auth payloads or pull Order into this scope. |
| [#27 KAFKA-01](https://github.com/duvanan/shopee/issues/27) | Order events remain outside this program. |
| [#31 OPS-03](https://github.com/duvanan/shopee/issues/31) | Reuse broker evidence where applicable; ELK tests remain separate. |
| [#24 RBAC-03](https://github.com/duvanan/shopee/issues/24) | Trace overlap with live authorization; avoid duplicate implementation/evidence. |
| [#1–#4 baseline](https://github.com/duvanan/shopee/issues/1) | Reuse valid audit/CI/Compose evidence, never infer Done from source existence. |

Search response state fields were sometimes null; do not infer issue state or completeness of repository inventory from them. PR44 merged status above is Coordinator-confirmed.

## Common Issue clauses

Copy these clauses plus the relevant row into each eventual Issue. Each row defines its goal/scope, file ownership, dependencies, deliverables, AC and mandatory tests. Exact filenames for new packages are proposals to confirm before delegation.

- Parent requirements: link BA/UX v1.0 and frozen technical contract revision. Record relevant TD-D/N-D/KN-D approvals explicitly. Backend-only slices mark UX implementation N/A while respecting externally visible UX contracts.
- Non-goals: no Order, Payment, Shipping, Push, In-App, account linking, social login, changed BA thresholds, direct-send delivery path, automatic synchronous fallback, production provider send, or unrelated refactor. A slice only edits its owned files. No live migration/backfill unless separately approved for that slice.
- Dependencies include G0: approved scoped design decisions, reviewed baseline, explicit Issue URL, scope/AC, assigned file owner, target/sprint/capacity and test environment. Missing parameters keep only affected slices blocked.
- Deliverables include code/config or scoped artifact named below, meaningful tests, BR/AC/UX mapping, actual command/results, migration/rollback notes, remaining risks and next-agent handoff. No secrets/credentials or raw personal data in Git/test artifacts.
- Build gates after implementation as applicable: `mvn -B -ntp verify -f backend/pom.xml`; `cd frontend` then `npm ci` and `npm run build`. These do not replace integration/fault evidence. QA independently verifies each slice; Security independently reviews sensitive changes. Fixes return to the implementer or a separately scoped Issue.
- Real providers/KMS credentials are not assumed. Use synthetic identities, fake adapters and controlled test-key fixtures until authorized provisioning exists. A fake-provider slice is not public-release readiness.
- PR links one implementation Issue, reports actual tests, migration/rollback, security findings and blockers. No automatic merge, close or Done. Any row exceeding 8–10h after detailed refinement must be split again before Ready.

## Auth drafts — itemized 86–126h

| ID / owner / estimate | Goal, owned files and deliverable | Dependencies | Acceptance criteria and mandatory tests |
|---|---|---|---|
| A1 Backend 6–8h | Audit/expand User schema; User Flyway/entity/repository; collision report and staged schema | TD-D05 | Canonical email/+84 uniqueness; no contact-to-login backfill or automatic mixed-kind repair. Fresh/legacy PG fixtures, concurrent unique insert, migration/restore rehearsal. |
| A2 Backend 8–10h | Durable session fence/atomic refresh; User Auth/Refresh/new SessionAuthorization service | A1; TD-D03/04/09 | PG revocation authority, Redis atomic generation,7d idle/30d cap. Replay, concurrent refresh/logout, Redis-write then PG-fail, multi-session and outage tests. |
| A3 Backend 8–10h | Gateway live check and trusted User/Product filters; service security files and app route policy | A2,A11a; TD-D03/04 | Strip spoofed identity even on public routes; live permission/401/403/503 behavior. Direct-service/cert-negative, multi-node revoke and dependency outage tests. |
| A4 Backend 8–10h | Quota reservations and transactional outbox primitives; User ledger/workflow/outbox schema | A1; TD-D06/08 | Atomic domain+outbox; email initial differs from resend; SMS budget enforced. Rolling boundaries/concurrency/rollback tests; delivery retries do not mint quota. |
| A5 Backend 8–10h | Email signup/verify/password setup/resend; dedicated User email controller/DTO/service | A4,N1a/N1b,N4a | 202 after PG commit, not provider ACK; pending has no session; scanner-safe owner password setup. Concurrent registration,24h/7d,old-link/replay,Kafka/provider-down tests. |
| A6 Backend 8–10h | Phone signup/login challenge; dedicated User phone controller/DTO/service | A2,A4,N1,N4 | No account before valid signup OTP; purpose-bound six digits/5m/five failures. Resend generation,concurrent activation,unknown phone,one-use/session tests. |
| A7 Backend 6–8h | Reset/profile/disable; User reset/User/Admin controllers/services | A2; TD-D01/02/05/09 | One-use reset, name-only allowlist, disable epoch. Concurrent reset,TTL,privilege injection,all-node disable tests. Split further if combined scope exceeds10h. |
| A8 Frontend 8–10h | Email/signup/verification/reset UI; email views and dedicated API/types | Stable A5/A7 contracts/fixtures | Email never auto-login;12–128 password;neutral202. Form,network,expiry,cooldown and UX email flow tests. Shared frontend files handed off sequentially to A9. |
| A9 Frontend 8–10h | Phone/session/profile/error UI; auth store/http/router/OTP/account/admin views | Stable A2/A3/A6/A7 contracts;A8 shared-file handoff | Await logout, safe return,no automatic add-cart,multi-tab token/session behavior,401/403. OTP/network/redirect/session tests. Re-slice rather than drop UX if oversized. |
| A10a QA/Security 6–10h | Auth API/security acceptance; User/Gateway test packages | Relevant A2–A7 slices | BR/AC traceability and executed ownership/401/403/revoke/race/anti-enumeration evidence. Findings are not self-approved implementation. |
| A10b QA 6–10h | Browser E2E/accessibility; frontend E2E fixtures/tests | A8/A9 | UX-ACC01..40 mapped;320/390px/desktop,keyboard,screen-reader,contrast,uncertain logout. Record actual results and gaps. |
| A11a DevOps 3–5h | Application workload identity deployment; User/Gateway/Product TLS runtime config/runbook | TD-D03 and approved CA/key provisioning | Wrong peer fails;private endpoints;no repo credentials. mTLS smoke and negative identity tests. Separate from Kafka broker certificates K2. |
| A11b Backend/DevOps 3–5h | Auth backfill/cutover rehearsal; scoped tooling/Auth rollout docs | A1–A10;TD-D05;owner review | Reviewed data disposition,legacy re-login,restore rehearsal,no JWT-only mixed fleet. No unapproved live backfill. |

## Notification increment — 48–72h

| ID / owner / estimate | Goal, owned files and deliverable | Dependencies | Acceptance criteria and mandatory tests |
|---|---|---|---|
| N1a Architect→Backend 4–6h | Frozen event/JIT schemas and synthetic fixtures; dedicated contract directory/common contract files | TD-D11;N-D01/06/07 | Canonical metadata/AAD/signature fields,versioning,error envelope. Valid/invalid/compatibility schema tests. |
| N1b Backend/Security 4–6h | Envelope crypto/key integration; dedicated contract crypto package/tests | N1a;N-D02/08 | Encrypted recipient/secrets;tamper rejection/key-id lifecycle;controlled local fixtures. Mutation/revoked-key/unavailable-key/redaction tests. |
| N2a Backend 4–6h | Notification module and independent DB/inbox; notification-service and sole backend/pom.xml module edit | N1a;independent DB role/config | Flyway,unique event/dedupe,no cross-DB writes. Fresh/upgrade/duplicate persistence tests. |
| N2b Backend 4–6h | Kafka ingress/durable offset prefix/worker claims; Notification consumer/repository/lease files | N2a,N1b,K1/K2 | Offset only after durable persisted prefix;bounded parsing;fenced claims. PG-down,crash-before-offset,rebalance,two-worker tests. |
| N3a Backend 4–6h | Pinned vi-VN templates/fake SMTP/SMS interfaces; Notification template/provider package | N1a,N2a;N-D05/07 | Template allowlist,synthetic inbox,no real send. Rendering,recipient tamper,timeout,idempotency fixture tests. |
| N3b Backend 4–6h | Dispatch/retry/UNKNOWN state machine; Notification scheduler/attempt package | N2b,N3a,N4b;N-D04/05/09 | Stable provider key,lease fencing,no blind UNKNOWN retry. ACK-then-crash,lease expiry,reconciliation,grant-expiry tests. |
| N4a Backend 4–6h | User Kafka relay; User outbox publisher package | A4,N1b,K1/K2 | acks=all,no mark before ACK,same event on republish. Pre/post-ACK crash,broker unavailable,claim takeover tests. |
| N4b Backend 4–6h | User JIT authorize/complete; private User notification authorization package | A4,N1a,A11a;N-D04/06 | Peer-bound grant,generation/deadline/reservation match,audit-only completion. Stale/resend/expired/wrong-peer/replay tests. |
| N5a QA/Security 6–8h | Application crash/race/secrecy suite; dedicated integration tests | A5/A6,N2b/N3b/N4 | Duplicate outbox/replay/restart,lag>5m,JIT denied,UNKNOWN,leak capture. Excludes broker quorum tests K4. |
| N5b QA/DevOps 6–8h | Latency/backpressure suite; workload fixtures/redacted reports | N5a,K3;TD-D12;N-D04/09 | Approved workload with2 Notification instances;commit→provider ACK stage p95/p99,queue age/expiry/resource report;no handset-arrival SLA claim. |
| N6a Backend/DevOps 2–4h | App cutover/event-compatibility flags; Notification/User rollout config | N5;owner review | Exactly one delivery owner;queued records re-authorize/expire;no dual-send. Controlled cutover/revert fixture rehearsal. |
| N6b DevOps 2–4h | App retention/restore; Notification/User cleanup jobs/runbook | N-D08 and approved keys | Ciphertext expiry cleanup,sanitized receipts,restore dispatch paused till reconcile. Cleanup/restore UNKNOWN rehearsal. |

## Kafka infrastructure — 24–36h

| ID / owner / estimate | Goal, owned files and deliverable | Dependencies | Acceptance criteria and mandatory tests |
|---|---|---|---|
| K1 DevOps 6–8h | Configuration-only Kafka3 topology/listeners/volumes and frozen topic declarations; Compose Kafka include and infra/kafka | KN-D01/02/04;approved trust contract;G0 | Three unique IDs/quorum,persistent per-node volumes,host/container metadata,RF3/minISR2 declared;config/schema/static checks. **Activation gated on K2: no plaintext listeners or runnable unauthenticated startup.** Live AdminClient topology/persistence validation occurs after K2. |
| K2 DevOps/Security 6–8h | Broker TLS/mTLS/ACL provisioning; infra/kafka identity/bootstrap files | K1;KN-D05;approved CA | Default deny,producer/topic/group least privilege,no plaintext fallback. Wrong CA/SAN/principal/topic tests plus activated K1 host/container metadata and persistence checks. |
| K3 DevOps 4–6h | Broker metrics/resource/recovery runbook; Kafka monitoring/runbook files | K1/K2;KN-D04/07/08 | Quorum/ISR/disk/age alerting,private management. Alert smoke and actual resource benchmark. |
| K4a DevOps/QA 4–7h | Real3-node one-broker failure/restart; isolated dedicated fault harness | K1/K2;app path for logical delivery assertions | ACK-event durability,leader recovery,ISR3 return,no storage reformat. Actual fault artifacts required. |
| K4b DevOps/QA 4–7h | Majority loss/network partition/DR; isolated harness | K4a | Block unsafe publish,no minISR downgrade,preserve volumes,paused restore/reconcile. Two-node-loss,partition and recovery rehearsal. |

K1 is the first potentially ready small implementation Issue after its scoped choices, reviewed baseline and capacity are approved. It must explicitly remain configuration-only until K2; its PR must not claim operational TLS/ACL/topology resilience evidence. If the team requires live topology acceptance in K1, K1/K2 must be re-sliced around secure bootstrap rather than run plaintext temporarily.

## Dependency order and ownership

```text
G0 reviewed baseline + scoped approvals + capacity decision
 +-- N1a contracts --> N1b crypto -----------------------+
 +-- K1 config --> K2 secure activation --> K3/K4a/K4b  |
 +-- A1 --> A2 --> A11a --> A3                         |
      +--> A4 --> N4a relay / N4b JIT -----------------|
N1 + K1/K2 --> N2a --> N2b --> N3b (also N3a,N4b) ----|
A2/A4/N1/N4 --> A5/A6/A7 --> A8/A9 --> A10a/A10b -----|
integrated evidence --> N5a --> N5b --> N6 --> A11b --> owner review
```

N3a can develop fake adapters/templates before Kafka readiness; actual dispatch requires durable inbox and JIT. Frontend may prepare approved-contract types/fixtures/UI without live APIs after freeze. Backend User and Notification can work alongside DevOps if ownership remains independent. Final delivery acceptance requires all integrated paths, not independent builds.

Critical path: approvals/baseline → contract freeze plus User data/quota/JIT and Kafka trust → durable inbox/dispatch → integrated Auth flows → fault/latency/security/mobile evidence → cutover review. Provider/KMS provisioning, data policy or machine budget can become the controlling branch. Effort ranges are not parallel elapsed-time promises.

Shared file arbitration:

- `backend/pom.xml`: N2a sole writer; dependency changes queued through owner.
- Root Compose, database bootstrap and `.github/workflows/ci.yml`: DevOps sole writer; Backend supplies config requirements. Prefer scoped includes/test scripts. DB provisioning request is owned by DevOps, Flyway schema by Notification Backend.
- `backend/common`: N1a contract owner; queue API/error changes sequentially.
- User `AuthService`, `AuthController` and migration sequence: A2→A4→A5→A6→A7 serialized, or approve package extraction before independent edits.
- Frontend `authApi`,store,http,router: single frontend owner or explicit A8→A9 handoff, no parallel edits.
- QA/Security can review read-only in parallel; implementation fixes return to owner. At most three subagents active.

## Capacity and unresolved estimate discrepancy

Auth A1..A11 rows sum **86–126h**, not the **90–130h** headline baseline in the draft. Notification adds48–72h, Kafka adds24–36h. Itemized total is **158–234h**; published headline is **162–238h**. The unallocated **4h remains unresolved**: Architect/Coordinator must name an integration allowance or correct the headline after review; this planner does neither silently.

Headline162–238h equals5.06–7.44 sprints at32 committed hours, approximately10.1–14.9 planned weeks. Spending all20h/week including buffer gives8.1–11.9 weeks but violates the committed planning assumption. Five MVP sprints have160h for the entire project: this program alone exceeds that by2–78h before Cart/Order/Inventory/Payment/QA. Eight Beta sprints have256h: this program uses63–93% before other work. Multi-agent parallelism is not approved additional human capacity.

Avoid double counting: A4 owns business quota/outbox basics only; N4 owns Kafka relay/JIT extension. Provider orchestration belongs N3, not A4. N5 tests application/provider faults; K4 tests broker/quorum faults. A11 owns Auth trust/backfill; N6 application retention/cutover; K2/K3 cluster identities/runbooks. Baseline evidence may be reused only when actual scope/results match. If independent per-slice review/integration takes more than package allowances, re-estimate explicitly.

Owner must choose a concrete capacity/replan treatment before commitment. Do not move Jan17/Feb28 dates, consume buffer as planned capacity, drop mandatory gates, or automatically remove Cart→Order/Inventory→Payment priority. All rows remain Unscheduled until that decision. A1 audit-only preparation can produce synthetic collision/migration evidence while TD-D05 is pending, but is not permission for live migration or full A1 completion.

## Evidence and handoff

Executed: static document/traceability/estimate reads, git status and branch diff, read-only GitHub issue/PR searches. No Maven/npm, migration, Kafka startup, provider, E2E, fault or benchmark tests were run by Planner. All mandatory tests in tables are future acceptance requirements, **Not Run**.

This task writes only this new planning document. No application files, existing docs, GitHub Issues, PRs, branches or approvals changed. Main risks: unresolved approval/contract parameters, baseline integration, capacity conflict, KMS/provider uncertainty, legacy data disposition, local resources, and oversized rows requiring refinement. Architect should resolve the4h estimate gap and contract blockers; Coordinator should present targeted approval choices, then create scoped implementation Issues only when DoR is demonstrably met.
