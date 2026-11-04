---
name: testing-strategy
description: "Use when creating a test plan, implementing automated tests, reviewing acceptance criteria or verifying Spring/Vue microservice features, especially auth, stock concurrency, payment idempotency and Kafka/Redis failure scenarios."
---

# Shopee Clone — Testing Strategy

## Trigger
Use for test design, regression suites, integration/E2E acceptance and failure testing before a Feature moves to Done. Skip for simple documentation updates with no behavior or delivery gate impact.

## Establish evidence
1. Read the GitHub Issue's measurable AC and `docs/project-management/WORKFLOW.md` DoD.
2. Identify dependencies: Postgres, Redis, Kafka, Eureka, Config Server, Gateway, frontend.
3. List current test sources and runnable commands before inventing a new framework.
4. Create a **test matrix**: scenario, layer, setup/data, expected result, run status/evidence, owner.

## Coverage strategy
- **Unit:** business constraints, pure mappings/validation, error-code mapping.
- **Repository/integration:** Flyway/schema, transactions, conditional stock update, Redis lifecycle; Testcontainers if project supports it or add as scoped work.
- **API/contract:** 2xx/4xx/5xx, pagination, Gateway routes, 401/403, ownership, DTO shape.
- **E2E:** Vue → Gateway → services → DB, realistic customer workflow and error UI.
- **Reliability:** duplicate requests/events, retry, timeout, concurrent orders, broker loss, consumer restart, refresh-token replay/rotation.
- Prefer deterministic fixtures, isolated DB/schema and cleanup; never require mutable personal data or credentials.

## Execute / report
- Backend: `mvn -B -ntp verify -f backend/pom.xml`.
- Frontend: `cd frontend && npm ci && npm run build`; run E2E runner if configured.
- Capture command, environment, test IDs, pass/fail/skip counts, actionable failure reproduction.
- A green build does **not** prove business E2E, security, race or failure acceptance.
- Clearly label **Not Run**, **Blocked**, **Failed**, **Passed** with evidence; do not infer Pass from source inspection.

## Done recommendation
For each AC cite its test evidence or identify the gap. Escalate critical failures to a follow-up Issue or block Done. The coordinator/human owns final acceptance.
