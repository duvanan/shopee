---
name: ecommerce-domain
description: "Analyze Shopee Clone e-commerce marketplace business requirements for catalog, cart, inventory, order, checkout, payment, shipping, reviews, sellers, promotions and returns. Use to define user stories, business rules, edge cases and acceptance criteria before architecture or implementation."
---

# E-commerce Domain Analysis — Shopee Clone

## When to use
Use this Skill when an Issue introduces or changes **e-commerce business behavior**: product/catalog, cart, inventory/stock, address, checkout, order, mock payment, order fulfillment, reviews, multi-seller, promotion, shipping, refund or return.

Do not use for infrastructure-only tasks (CI, Kafka cluster setup, ELK) unless they require interpreting an explicit commerce business rule. This Skill does **not** run a business-analysis agent by itself; the coordinator must delegate and the agent must read it.

## Project facts and scope boundary
- Shopee Clone is a learning project, not a statement about the real Shopee platform's internal behavior.
- **Current implemented baseline**: auth, roles/permissions, users, product/category catalog and admin product/category management (verify against the latest code and tests for actual runtime status).
- **MVP target**: Cart → Order/Inventory → mock Payment/Checkout → end-to-end purchase and order history; follow the current sprint plan before changing scope.
- **Potential later extensions**: Seller/multi-vendor, promotions/vouchers, search Elasticsearch, real shipping, returns/refunds. These are **not** implicitly MVP requirements.
- Treat historical `docs/ROADMAP.md` / `docs/features/*` as context; when they differ from source, report the difference. Do not silently equate an implementation detail with user-approved business policy.
- Existing roles include `CUSTOMER` and `ADMIN`; `SELLER` is a possible extension, not necessarily an implemented actor.

## Domain checklist (only include relevant concepts)
| Domain | Questions to resolve before technical design |
| --- | --- |
| Catalog & pricing | Visible/hidden/unavailable products? One price or variants? When may price change? What do buyers see? |
| Cart | Guest or authenticated cart? Item quantity bounds? Removed product? Repricing? Cart persistence and multiple devices? |
| Inventory | What counts as available stock? When reserve/decrement/release? Oversell prevention? Partial stock and simultaneous orders? |
| Checkout & addresses | Required buyer/address fields? Shipping fees, taxes, currency and price breakdown? Review-before-submit rules? |
| Orders | Status lifecycle and allowed transitions? Order item price snapshot? Cancellation, expiry, duplicate submission? Ownership and admin views? |
| Payment | Mock or real gateway? Paid/failed/pending semantics? Retry/idempotency, timeout, cancellation and reconciliation? |
| Shipping | Real carrier integration or mock tracking? Seller fulfillment vs platform fulfillment? Tracking events and SLA? |
| Reviews | Who may review? Rating bounds, duplicate reviews, edit/delete/moderation and aggregate freshness? |
| Seller/marketplace | Multi-seller cart? Split orders and payouts? Product ownership? Seller role onboarding? **Out of MVP unless approved.** |
| Promotion | Eligibility, stacking, usage limits, expiry, inventory interactions, auditability? **Out of MVP unless approved.** |
| Return/refund | Return window, reasons, partial refund, inventory return and who approves? **Out of MVP unless approved.** |

## Analysis workflow
1. **Ground the request.** Read Issue, actor, scope, constraints, existing APIs/UI/docs and roadmap. Label each claim as **implemented**, **approved**, **proposed**, or **unknown**.
2. **Map the journey.** Describe the happy path from the actor's perspective (trigger → actions → system-visible outcome), then alternative/error paths. Keep ownership and permissions visible.
3. **State business rules (BR-xx).** Define preconditions, invariants, allowed states, validation and exception outcomes without prescribing code structure.
4. **Make cross-feature interactions explicit.** Cart → pricing/stock → checkout → order → mock payment → notifications; identify where users see changes and what happens when a step fails.
5. **Explore edge cases.** Out-of-stock, price changed, stale product, multi-tab checkout, duplicate clicks, replay, insufficient stock, payment pending/failure/timeout, access to another user's order, cart lost/expired, cancelled order and subsequent stock release.
6. **Write verifiable acceptance criteria.** Prefer concise Gherkin Given/When/Then: user-visible behavior and expected API errors; avoid mixing implementation choice into acceptance criteria.
7. **Surface unanswered questions.** Give 1–3 options where useful, recommend a default labeled **proposal pending approval**, and flag decisions that affect scope or data consistency. Never silently decide business policy.
8. **Handoff to architect/planner/QA.** Architect gets rules/state transitions and desired consistency, Planner gets estimate/dependency impacts, QA gets a scenario matrix. Do not mark the Issue Ready if blocking business rules remain unresolved.

## Required output template

```markdown
# BA Spec — <FEATURE-ID>: <Feature>
Status: Draft / Needs approval / Approved (only if user approved)
Sources: Issue link, current code/docs reviewed

## Problem & scope
Business goal / actors / in scope / out of scope

## User journeys
Happy path, alternative paths, error paths

## Business rules
BR-01 ...
BR-02 ...

## Lifecycle & dependencies
Domain entities, state transitions, upstream/downstream features

## Acceptance Criteria
- AC-01: Given ... When ... Then ...
- AC-02: ...

## Edge-case matrix
Scenario | Expected business outcome | Priority | Test hint

## Open questions requiring approval
- Q1: ... Options ... Recommended default (not yet approved)

## Handoff
Architect: consistency/contract implications without prescribing technology
QA: tests for each AC and business invariant
Planner: scope, estimate/deadline risks
```

## Example: Cart business decisions (not automatically approved)
- **BR proposal:** an unavailable or deleted product cannot be checked out. Clarify whether it stays visible in cart with an error or is automatically removed.
- **BR proposal:** cart display price can change; server rechecks price at checkout and asks user to confirm any difference. Clarify what is persisted on order placement.
- **BR proposal:** adding quantity above current stock is rejected or capped with a clear message. Choose one as a business decision; do not assume.
- **MVP split:** Single platform catalog with CUSTOMER/ADMIN roles now; multi-seller split orders/shipments are separate future capabilities.

## Quality gates
- Every AC maps to at least one QA test scenario.
- Separate business validation (`400/409` as contract outcome when agreed) from unknown technical failure handling; never promise successful payment without actual payment state.
- Identify privacy/security ownership checks (users must not see another customer's cart or order).
- Confirm deadline/dependencies and flag scope expansion in the existing sprint, rather than silently accepting extra work.
- Requirements are not finalized until an authorized human has approved blocking decisions.
