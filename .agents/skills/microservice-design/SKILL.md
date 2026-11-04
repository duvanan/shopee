---
name: microservice-design
description: "Use when designing or changing Shopee Clone microservice boundaries, REST contracts, database ownership, Redis/Kafka integration, transactions, concurrency or failure recovery before implementation. Not for UI-only copy changes."
---

# Shopee Clone — Microservice Design

## Trigger
Use for a new service, cross-service API, schema change, event-driven workflow, or consistency-sensitive operation (Cart, Order, Inventory, Payment). Do **not** invoke for routine formatting or isolated UI text changes.

## Inputs to inspect
1. GitHub Issue (Feature ID, Acceptance Criteria, deadline, dependencies) and `docs/project-management/WORKFLOW.md`.
2. Existing `backend/pom.xml`, `backend/api-gateway`, `backend/common`, relevant services' Controller/Service/Repository and Flyway migrations.
3. `docs/architecture/rbac.md`, `docs/architecture/api-response-standard.md`, `docs/ROADMAP.md` as **context**. Reconcile older docs against source code, which takes precedence for current implementation.

## Workflow
1. **Boundary:** name owning service, its data, actors, API exposure, dependencies, and which component validates each business rule.
2. **Contracts:** HTTP method/path, request/response shapes, pagination, error codes, authentication, authorization, ownership, timeouts and idempotency key strategy.
3. **Data:** tables/constraints/indexes/Flyway change, flat FK IDs rather than implicit JPA object graphs, Redis keys/TTL or Kafka topic/key/schema/retention as relevant.
4. **Sequence:** draw request/event steps and state transitions; identify transaction boundaries and possible partial failure.
5. **Failure matrix:** handle duplicate request, retry, timeout, concurrent writes, service down, network partition and replay; document guarantees vs tradeoffs.
6. **Security:** verify forwarded user context can only originate from a trusted Gateway/internal channel. Never treat raw external `X-User-*` headers as authoritative.
7. **Scope:** propose smallest slice satisfying the Issue; list out-of-scope enhancements and dependencies, rather than overbuilding.

## Deliverable
Provide a concise design note with:
- Context & goals / non-goals.
- Components, ownership and sequence/data flow.
- API + data/event contract and error cases.
- Security and consistency decisions (alternatives when tradeoff matters).
- Implementation breakdown and **specific tests/acceptance evidence**.
- Risks, unresolved decisions, estimated effort impact.

## Shopee-specific checks
- Use `backend/common` API response and trace ID conventions.
- Only the appropriate service owns and migrates its database; no cross-service DB writes.
- For stock/payment, specify concurrency and idempotency, not just a happy-path diagram.
- For Kafka, distinguish durability, delivery, consumer idempotency, DLQ and outbox needs.
- Do not declare a design production-safe without tests and operating assumptions.
