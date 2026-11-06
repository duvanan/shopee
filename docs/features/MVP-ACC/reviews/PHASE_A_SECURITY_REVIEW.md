# MVP-ACC — Phase A independent security review

- Issue: [#43, Architect-only](https://github.com/duvanan/shopee/issues/43).
- Date: 2026-11-06; reviewed handoff branch `chore/MVP-ACC-approved-specs-architect-handoff`.
- Reviewer: custom `security` agent, actually executed by Coordinator delegation.
- Evidence level: static document/source inspection only. No runtime security sign-off.
- Skills read: `.agents/skills/security-review/SKILL.md`, `.agents/skills/testing-strategy/SKILL.md`.

## Approval boundary

The owner explicitly selected Kafka-first: User-owned Account/Auth/OTP/quota and transactional outbox → Kafka3 → independent Notification DB/inbox/workers; User-only verification/activation/session issuance; JIT delivery authorization; Gateway JWT/live-session/trusted identity checks; Redis opaque rotating refresh tokens; HTTP202 after User PG commit; no direct-send/hybrid primary path or automatic synchronous fallback. These architectural principles do not require another choice of primary delivery path.

The chosen PG session fence/live authorization/Redis principles are retained. Detailed mTLS identity deployment, availability tradeoffs, immediate-revocation acceptance interpretation, policy parameters, providers, keys and pending TD/N/KN decisions still need scoped approval/contract closure. BA/UX approval is not approval of all Draft TD parameters. This report changes no approved business rule and does not mark the TD Approved.

## Prioritized findings in existing source

Severity below is an implementation/acceptance priority, not a claim that a runtime exploit was reproduced. Paths and line numbers refer to inspected source.

| ID / severity | Evidence and impact | Required fix and regression evidence |
|---|---|---|
| SEC-A01 / P0 | `backend/user-service/src/main/java/com/shopee/user/security/HeaderAuthenticationFilter.java:29` and Product equivalent `:28` build authentication from client-supplied `X-User-Id`/roles without checking peer identity. Gateway `JwtAuthenticationFilter.java:73` forwards public routes before sanitization. If backend is reachable, forged headers can cross the assumed trust boundary; network reachability/exploit not tested. | mTLS peer allowlist, sanitize identity/internal headers before every route, deny untrusted direct callers. Test missing/wrong certificate, duplicate/case-variant headers, public-route header forwarding and unauthorized workload. |
| SEC-A02 / P0 | `backend/api-gateway/src/main/java/com/shopee/gateway/filter/JwtAuthenticationFilter.java:99` verifies JWT and uses embedded permissions without live session/account authorization. `backend/user-service/src/main/java/com/shopee/user/service/AuthService.java:158` refreshes without checking account status. Existing bearer authorization does not implement immediate revoke semantics. | PG primary session/account fence and live checks; old bearer denied on two Gateway/User nodes after logout/disable/permission commit; dependency timeout fails closed. Do not substitute JWT TTL/cache expiry for revoke. |
| SEC-A03 / P0 before real use | `backend/user-service/src/main/java/com/shopee/user/service/PasswordResetService.java:73` logs email and raw reset token. A log reader could obtain a still-valid reset credential. No log runtime captured. | Remove secret logging; capture application/access/trace/error logs and artifacts with synthetic fixtures; zero OTP/token/secret URL/cookie/recipient leakage. Do not replace with raw ciphertext/provider-error logging. |
| SEC-A04 / P1 | `backend/user-service/src/main/java/com/shopee/user/security/RefreshTokenService.java:78` rotation and `:173` persistence use separate Redis get/delete/set operations, without CAS, durable generation or replay tombstones. Expiry advances by another30d without absolute cap. Concurrent refresh/logout can race. | Account→session locks, atomic Redis compare/rotate, durable generation, tombstones, idle7d/absolute30d. Barrier tests: concurrent refresh, logout/disable race, Redis success→PG commit failure, restart/restore; no resurrection. |
| SEC-A05 / P1 | `backend/user-service/src/main/java/com/shopee/user/service/PasswordResetService.java:80` checks then consumes without row locking/CAS. `:83` uses `isBefore`, so equality is not explicitly expired. | Atomic consume, exactly one concurrent success, reject `now >= expiresAt`, generation invalidation; approved reset TTL/revoke policy. |
| SEC-A06 / P1 | `backend/user-service/src/main/java/com/shopee/user/security/SecurityConfig.java:26` disables CSRF and `:29` permits auth wildcard. Refresh cookie has HttpOnly/SameSite=Lax but Secure defaults false in `RefreshTokenService.java:54`. No confirmed CSRF exploit. | Endpoint/method allowlist, signed nonce/header+exact Origin contract, explicit credentialed CORS, deployment-required Secure/host-only/Path attributes. Test foreign/sibling Origin, missing/expired/mismatched CSRF, duplicate cookies and proxy transport. |

## Draft design assessment and acceptance gates

No additional P0/P1 contradiction was established in this static review of the three TDs. Planned controls are not implemented evidence. The following gates must be closed for affected slices; they do not reopen the owner's Kafka-first choice.

| Gate | Decision/contract owner | Required evidence — all Not Run |
|---|---|---|
| Trust deployment | Architect+DevOps+Security; owner accepts detailed TD-D03/KN-D05 deployment/availability policy | Freeze certificate→workload→endpoint/topic/group mapping. Wrong CA/SAN/expired/revoked cert rejected; internal endpoints never public; least-privilege producer/consumer ACLs; no plaintext fallback. |
| Crypto/key lifecycle | N-D02/TD-D11; Architect contract, owner local/trial/prod key-management choice | Executable byte-level RFC8785/JCS, AAD, signature and requestDigest fixtures; duplicate JSON keys/lone surrogates invalid; tampered metadata/payload never reaches provider. Separate signing/KEK/HMAC/JWT keys; rotation/revoke/key-manager outage fail closed; fake-only material outside Git. |
| JIT/attempt contract | Architect+Backend; TD-D11/N-D04/N-D06 details | Freeze deny HTTP/error codes, outcome enum and reconciliation semantics. Same attempt returns original grant/deadline; changed digest rejected; expired/replaced/consumed/locked deny. Lost authorize response never permits unconfirmed send; retry cannot renew grant indefinitely. |
| Worker uncertainty | Backend+QA+Security; N-D05 | Separate provably pre-dispatch work from SENDING uncertainty. Lease expiry/crash after provider acceptance becomes UNKNOWN; no blind second send or vendor switch. Stable providerKey and adapter-documented dedupe/status window; disable hidden SDK retries. |
| Lock order | Architect+Backend | Freeze global order spanning rate/workflow/account/session/token paths; transaction barriers exercise resend/verify/cleanup/login/refresh/disable and bounded deadlock retry before external I/O. |
| Idempotency conflicts | Architect+Backend | Define same producer+idempotencyKey with changed eventId/digest rejection/quarantine, not silent merge; eventId/digest mutation and replay fixtures; no raw AuthResult persisted/replayed. |
| Retention/replay | Owner N-D08/KN-D03; Architect+DevOps | Retention horizon covers supported replay/outbox retry windows; ciphertext/key/backup/inbox/grant/UNKNOWN retention specified. Restore pauses delivery until reconciliation; sanitized-only retry/DLQ; expired auth never replayed. |
| Quota/abuse | Owner TD-D06/TD-D08; Backend+QA | Rolling-window and failed/unknown reservation counting policy approved. Shared phone signup/login limits, canonical aliases and HMAC rotation cannot reset counters;50 concurrent requests honor BA quotas; synthetic identities follow equivalent public behavior. |
| Session/revocation | Architect+Backend; owner TD-D04 interpretation and detailed outage acceptance | Two-node immediate postcommit deny, no positive cache, primary reads, explicit in-flight request semantics. Concurrent retry/replay distinguishes409 conflict/lost response/malicious replay without two sessions or expanded replay grace. |
| Fault/resources | DevOps+QA+Security; KN-D01/04/07/08 parameters | Three real brokers, RF3/minISR2/acks=all; stop one/restart/rebalance and stop two/partition; no false publish success, no lowered ISR. PG/Redis/JIT/Notification DB outages fail closed; ACKed event recovery under stated storage assumptions. |
| 202 and anti-enumeration | Backend+Frontend+QA | User PG commit enables202 independently of Kafka/Notification/provider ACK; same neutral accepted contract for synthetic/no-op; no sent=true/provider status leak; timing/response and outage admission tests. |

## Residual risks and accepted-choice limits

- Auth TD:293 and Notification §7.3 correctly describe the check–send race. JIT5s/DB fences are not checked by the provider. A process paused after its last local deadline check, or an already accepted vendor request, can dispatch/deliver an old message. Owner must accept these detailed semantics; User must always reject the old generation after resend commit. Do not assert zero stale physical delivery.
- Notification is trusted delivery infrastructure: it can decrypt secrets and holds provider credentials. User event+attempt uniqueness does not enforce honesty of a compromised Notification worker. Least privilege, cost caps, credential/key rotation and incident stop procedures remain necessary.
- UNKNOWN without provider-supported evidence/deduplication sacrifices delivery availability to avoid unsafe duplication. SMTP ACK is not handset/mailbox confirmed delivery.
- Three containers on one host do not tolerate host/disk loss. Encryption and Kafka retention do not prove cryptographic erasure from all backups.
- Owner-selected architecture does not settle reset TTL/session revoke policy, legacy migration, quotas/windows/refunds, abuse thresholds, key management, real provider budget, retention or milestone capacity.

## First slice and handoff

Prepare a narrowly scoped contract/security-fixture slice after a reviewed baseline and the relevant key/contract decisions: schema/crypto fixtures, fake-only key/provider policy, executable negative cases. No real provider is required. A separate urgent security-maintenance issue removing raw reset-token logging is also independently preparable; do not put production implementation in Architect-only#43.

Architect was sent the source findings and trusted-Notification limitation. Architect returned closure tasks for executable JIT outcomes, worker uncertainty phases, crypto fixtures, global lock order, idempotency conflict and replay horizon; these are incorporated above as DoR gates, not new owner-approved policies.

## Execution evidence

- Executed: read-only `Get-Content` and `rg` over repository workflow/role/skill files, approved BA/UX context, all three technical designs and the source paths above.
- Not Run: Maven verify, npm build, integration/E2E, Kafka fault/load, CSRF/identity exploitation, provider/KMS sandbox and secret-redaction runtime tests.
- No real SMS/email, credentials or provider calls used.
- Original design/application files unchanged. Only this new report was written on explicit Coordinator assignment; no staging/commit/GitHub mutation.
- No implementation security sign-off, Issue closure, merge or TD approval granted.
