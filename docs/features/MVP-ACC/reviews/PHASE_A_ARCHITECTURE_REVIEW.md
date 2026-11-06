# MVP-ACC — Phase A Architecture Review

Status: **Draft recommendations / Needs owner review**. Issue: [#43 — Architect only](https://github.com/duvanan/shopee/issues/43). Review date: 2026-11-06.

This report records the actual Architect static review. It does not approve the three Technical Designs, change approved BA/UX, authorize baseline integration, or claim runtime validation.

## Inputs and baseline

Read repository workflow, MULTI_AGENT, SPEC_HANDOFF, roadmap, Architect role, microservice-design and security-review skills; approved BA/UX, Issue #43 supplied in Coordinator context, the three draft designs and relevant source. BA approval evidence is recorded in BA_SPEC.md; UX approval evidence records the owner's 05/11/2026 confirmation. Existing source, not target design, determines implementation status.

Coordinator fetched both remote branches. Static Git comparison: origin/main and handoff diverge 1/1: main has merge commit `8f2e176`; handoff has design commit `b490804`. Tree difference contains three new design documents and MVP-ACC README. No merge/rebase/cherry-pick was performed by this reviewer.

## Architecture conclusion and approval boundary

The three documents consistently specify User PG transaction → 202 → outbox publisher → Kafka → Notification durable inbox → JIT authorization → provider. 202 is independent of Kafka/Notification/provider ACK. User alone verifies credentials, activates accounts and issues sessions. Notification owns delivery/attempts/providers and its own database. Kafka append and provider acceptance are not atomic with User PG.

The current owner instruction selects Kafka-first, User ownership, PG transactional outbox, independent Notification inbox/idempotency/workers, JIT, Gateway JWT/live session authorization/trusted service identity, opaque Redis rotation, encrypted events, no automatic synchronous fallback, and Email/SMS-only current scope. Do not ask the owner to select these again. This does **not** approve every Pending value, PKI deployment, data policy, provider or schedule. Exact mTLS topology/CA mapping remains a deployment decision; the abstract trusted-service requirement is settled.

Key consistency references: Auth TECHNICAL_DESIGN.md §4.4–4.5; Notification §3, §6–7; Kafka §6–8. The digest is SHA-256 of UTF-8 RFC8785 canonical original envelope excluding signature, including ciphertext. Grant binding is event/attempt/fence/generation/digest/principal; original grant deadline does not extend on retry.

Resend invalidates old generation at User commit. Already authorized/in-flight stale messages can still arrive; a process can pause after its last local deadline check. DB leases/fences do not fence a provider socket. Old credentials remain invalid at User after resend. UNKNOWN cannot be blindly retried or moved to a different provider. Notification is a trusted component with access to decrypted delivery secrets: User grant uniqueness does not make a compromised Notification worker honest.

## All 30 decisions

All IDs retain their original document status until approval evidence is recorded. “Selected subset” describes this conversation only. Engineering closure means specialist-owned detail, not a new owner business decision.

| ID | Selected subset / recommendation still requiring closure | Gate and responsibility |
|---|---|---|
| TD-D01 | Reset TTL30m, one-use | Owner policy; reset API/UI/tests |
| TD-D02 | Reset revokes all sessions via epoch; no auto-login | Owner policy; reset/session acceptance |
| TD-D03 | PG fence/live authorization, Redis rotation and trusted identity selected; exact mTLS/CA deployment remains | Engineering deployment proposal plus owner operational acceptance; session/trust integration |
| TD-D04 | Revoke applies to checks starting after commit; preauthorized requests may finish | Owner interpretation; revocation acceptance |
| TD-D05 | Legacy proof audit; unclear→verify/reset; mixed Admin/Customer manual resolution | Owner migration policy; audit can be prepared, backfill/cutover blocked |
| TD-D06 | Rolling1h/24h; failed/UNKNOWN reservations count; email initial excluded; re-register counts resend | Owner quota semantics; send ledger/UI limits |
| TD-D07 | Real provider/budget/sender/support missing; fake adapters explicitly permitted | Only real delivery/public trial blocked; no unauthorized real sends |
| TD-D08 | IP/global caps, current email5-fail/15m lock, audit retention | Owner abuse policy; do not silently enable proposed20/min/IP |
| TD-D09 | JWT5m, Unicode code-point counting, profile≤255, reset replacement, idempotency24h; Argon2id benchmark | Owner confirms behavior defaults; engineering chooses measured encoder config |
| TD-D10 | Kafka-first/Auth Email+SMS selected;162–238h scheduling not approved | Owner replan/sprint allocation; no automatic deadline change |
| TD-D11 | Encryption/inbox/JIT selected; key mechanism, grant5s, retention and stale-delivery residual remain | Owner threat/acceptance choices plus engineering crypto contract |
| TD-D12 | No automatic sync fallback selected; targets/runway/backpressure remain | Owner performance acceptance; sync alternative outside current implementation |
| N-D01 | Shared private Notification/User boundary/Kafka selected | Engineering service/DB deployment detail; no repeat Kafka-vs-HTTP question |
| N-D02 | A256GCM envelope/Ed25519 recommendation, external key material | Owner local/prod key management/access/backup; engineering byte fixtures |
| N-D03 | Polling relay, inbox-before-offset, DB retry scheduler recommendation | Engineering contract approval/tuning; no unreviewed CDC scope |
| N-D04 | JIT5s/runway30s, p95≤5s/p99≤15s and stale-send residual | Owner acceptance thresholds; OTP worker gate |
| N-D05 | No blind UNKNOWN retry selected; stable providerKey required | Engineering fake fault tests; real provider dedupe/query evidence pending |
| N-D06 | JIT/trusted workload selected; exact mTLS mapping and errors/outcomes remain | Engineering private API fixtures; deployment policy review |
| N-D07 | Pinned vi-VN Email/SMS templates; future domains/channels excluded | Owner content/ownership review; not a module skeleton blocker |
| N-D08 | Body cleanup; inbox metadata30d; topic24h/signals7d; no expired replay | Owner retention/backup/replay policy; engineering cleanup consistency |
| N-D09 | Max3 safe attempts, provider3s, poll250ms/batch20/lease30s proposals | Owner operational acceptance; engineering benchmark/config |
| N-D10 | Small issues and independent QA/Security selected | Owner allocation remains; engineering dependency/file ownership |
| KN-D01 | Kafka3 selected; local combined/static quorum proposal remains | Owner topology acceptance; engineering compatible patch/digest/format verification |
| KN-D02 | Domain auth topic/group/pools;6partitions proposal | Engineering exact topic/ACL contract; future topics not provisioned |
| KN-D03 | Retention/key/backup/dedupe replay horizon | Owner data policy; cleanup/storage/replay gate |
| KN-D04 | Proposed Docker12GiB/free disk20GiB, broker budgets unmeasured | Owner resource budget; engineering actual local qualification |
| KN-D05 | Authenticated TLS/mTLS requirement details and default-deny ACL | Owner PKI mechanism/ownership; engineering principal mapping/negative tests |
| KN-D06 | Durable inbox/no blind UNKNOWN semantics selected | Engineering scheduler/signal-only DLQ contract |
| KN-D07 | Throughput/latency/recovery targets unapproved | Owner acceptance; no fallback implementation or TTL extension |
| KN-D08 | Kafka-first dependency selected;24–36h allocation and single-host DR risks unapproved | Owner roadmap/RPO/RTO; do not claim three containers tolerate host loss |

Source decision tables: Auth §12; Notification §12; Kafka §12. Approval of a deployment mechanism does not approve unrelated policies in the same row.

## Six actionable engineering closure tasks

1. **JIT executable contract:** freeze request/response schemas, deny HTTP mapping, bounded reason enums and `complete.outcome` transition/reconciliation rules. Recommended distinction:403 unauthenticated/unauthorized workload,409 immutable-binding conflict,503 dependency, one domain-denial schema with allowlisted reason. Exact domain-denial status must be uniform across implementations. Late UNKNOWN→accepted evidence must be defined; callback never mutates account/token/session/quota.
2. **Worker recovery before provider dispatch:** current durable SENDING-before-JIT is conservative, but JIT denied/crash-before-send needs an explicit NOT_DISPATCHED versus MAY_HAVE_DISPATCHED distinction. Phase metadata is an engineering recommendation, not approval. Absence of DISPATCH_STARTED proves safe only if every provider I/O requires that durable transition to commit first, stale workers are fenced before that transition, and no hidden SDK retry can bypass it. Otherwise UNKNOWN remains the default. A DB phase does not fence external provider I/O. Safe retry requires proof of nonacceptance or vendor dedupe, never lease expiry alone.
3. **Crypto byte contract:** freeze exact canonical AAD object and field inclusion, wrap mechanism/key IDs, decoded length/size limits and non-secret decryptable interoperability fixtures. Notification §4 already specifies AES-256-GCM/nonce96/DEK256 and §3 JCS; REDACTED placeholders are not interoperability tests. Include tamper, duplicate-key, unknown-key/schema and revocation fixtures.
4. **Global lock-order table:** specify one order covering identity rate lock, workflow, account, session and token for signup/resend/verify/cleanup/disable/refresh. Analyze cycles rather than assigning independent orders per Backend slice; add deterministic parallel race/deadlock tests.
5. **Business-idempotency conflict:** define same producer+idempotencyKey with different eventId/digest as reject/quarantine, not silent merging. Existing same-event/different-hash protection alone does not specify this case. Include unique-index concurrency fixtures and no-provider-call assertions.
6. **Retention/replay table:** align inbox/dedupe tombstones with every allowed outbox/manual replay horizon.30d metadata cannot safely justify unbounded replay; expiry/JIT remains authoritative after purge. Owner selects horizons/backup policy; Architect freezes table before cleanup jobs.

These close technical handoff details without changing approved BA/UX. Exact policy values remain owner decisions. Architecture review/contract drafting may continue while affected production slices wait.

## Estimate arithmetic and execution handoff

Verified A1..A11 rows in Auth §10 sum to **86–126h**, while headline Auth baseline says90–130h. Adding platform48–72h and Kafka24–36h yields **158–234h itemized**, versus headline162–238h. The unexplained4h must be reconciled explicitly by Planner/owner; do not silently invent contingency or reduce the program headline. Both ranges exceed the five-sprint160h committed capacity for the whole existing MVP plan, before Cart/Order/Payment work.

After baseline and slice-specific gates: freeze contracts → parallel secure Kafka/DB foundation and User audit/schema → session fence/Gateway trust and notification inbox → User workflows/quota/outbox/JIT plus Notification workers/providers → frontend after stable API fixtures → real three-node fault/E2E/security evidence → owner rollout review. Shared AuthService/controllers/common contracts/Compose/CI each have one owner at a time.

First candidate implementation slice: Kafka K1 configuration-only foundation6–8h after relevant topology/resource decisions and valid baseline approval. Running the cluster requires K2 TLS/ACL/trust gates; no plaintext-first integration. Coordinator measured host RAM16959066112 bytes (about15.79GiB), but Docker daemon was unavailable (`docker_engine` pipe absent), so Docker allocation and cluster feasibility remain unverified. Until ready, contract closure and audit planning remain unblocked. Issue #43 remains Architect-only; implementation requires separate Issues.

## Actual evidence and limitations

Executed static source/document reads, `git diff --name-status origin/main origin/chore/MVP-ACC-approved-specs-architect-handoff`, `git status --short`, and manual estimate summation. Reviewed current parent POM, response envelope, Gateway filter and User header trust filter against design. Existing trust/session defects require Security-led implementation gates; no runtime exploit is claimed.

No application build, migration, provider send, Kafka startup, broker failure, performance, or runtime security test was run. Report creation is the only file change by this reviewer. No staging/commit/GitHub mutation, merge, Issue closure or Technical Design approval. Next handoff: Coordinator policy table and Planner breakdown; Security reviews crypto/trust/recovery contract closure before implementation.
