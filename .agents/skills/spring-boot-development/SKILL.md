---
name: spring-boot-development
description: "Use when implementing or modifying Java/Spring Boot backend services, controllers, security integration, PostgreSQL/Flyway migrations, Redis persistence or Kafka handlers in Shopee Clone. Not for frontend-only work."
---

# Shopee Clone — Spring Boot Development

## Trigger
Use for backend implementation tasks with Java 25, Spring Boot 4.1, Spring Cloud 2025.1, Maven multi-module. For cross-service design first apply `microservice-design`. Do not use for docs-only or frontend-only work.

## First read
- Assigned GitHub Issue, AC and `docs/project-management/WORKFLOW.md`.
- Relevant `backend/<service>/src/main/java/`, `backend/<service>/src/main/resources/`, migrations, `backend/common`, `backend/pom.xml`.
- Gateway route/authorization configuration when adding a new endpoint.
- Inspect the code rather than assuming `docs/ROADMAP.md` reflects today's implementation.

## Implement in vertical slices
1. List expected endpoint(s), DTO validation, success/errors and authorization rules.
2. Implement database migration before relying on new persistence. Add constraints, indexes and safe forward migration notes.
3. Keep Controller thin, Service business logic explicit, Repository isolated. Follow existing flat-FK approach (no implicit JPA relationship by default).
4. Use `ApiResponse`, `BusinessException`/`ErrorCode`, 1-based `PageData` and trace ID conventions.
5. Preserve Gateway → backend trust boundaries. `X-User-Id`/`X-User-Permissions` must be validated as trusted context, not accepted unconditionally from exposed client-facing service ports.
6. Be explicit about `@Transactional` boundaries, especially native `@Modifying` queries; don't assume a DB transaction spans other microservices.
7. For Redis/Kafka: define failure fallback, TTL, retry/replay and idempotency before adding handlers.
8. Implement focused unit tests and integration/contract tests for success, validation, error, unauthorized, ownership, duplicate and race cases relevant to the Issue.

## Verify
- `mvn -B -ntp verify -f backend/pom.xml`
- Run feature-specific integration tests only when supporting services are available. State exact command, results and environment limits.
- Validate any new route through the API Gateway for public, authenticated and permission-required methods.
- Review diffs for secrets and unrelated changes.

## Output / handoff
Summarize changed file paths, API contract, DB migration/rollback considerations, tests executed with outcome, tests **not** run, risks/blockers and follow-up Issues. Do not claim Done, merge, or close the Issue without reviewed evidence.
