# AGENTS.md — Shopee Clone Coding Agent Guidelines

Instructions for Codex or other coding agents working on this repository.

## Required workflow
1. Read `docs/project-management/WORKFLOW.md`, `ROADMAP_2026_2027.md` and the **specific GitHub Issue** before changing code.
2. One Issue / one narrowly scoped change set at a time. Never implement an entire sprint in one PR.
3. Before coding, summarize requirements, dependencies, affected services, API contract, edge cases, tests and risks. Point out unknowns rather than silently inventing business rules.
4. Work in `feature/<FEATURE-ID>-slug`, `fix/<FEATURE-ID>-slug` or `chore/<FEATURE-ID>-slug` branched from main. Never directly commit to main for feature delivery.
5. Do not mark an Issue Done unless acceptance criteria + Definition of Done are actually validated.
6. Create/prepare a Pull Request linking the Issue. Do not merge or close the Issue without user review.
7. Do not commit secrets, tokens, passwords, local database data, node_modules, built artifacts or environment-specific credentials.

## Tech conventions (current repository)
- Backend: Java 25, Spring Boot 4.1, Spring Cloud 2025.1, Maven multi-module.
- Existing services: Eureka, Config Server, API Gateway, User, Product, Common. Design *new* modules consistently with these.
- Frontend: Vue 3 + TypeScript + Vite + Tailwind + Pinia.
- PostgreSQL database migrations via Flyway. Do not rely on Hibernate `ddl-auto` to create production schemas.
- Avoid implicit JPA object graph relationships; prefer flat FK IDs and explicit queries (current architecture choice).
- JWT validation and route-permission checks live in API Gateway; services should validate authorization context **only from trusted Gateway or authenticated internal calls**. Do not trust arbitrary client-supplied X-User-* headers; highlight trust-boundary findings.
- Auth uses short-lived access JWT + opaque rotating refresh cookie in Redis. Preserve cookie security attributes and avoid exposing refresh token to JavaScript.
- API contracts must follow `backend/common` response/error and pagination conventions.
- Trace/correlation ID should survive across service boundaries.

## Build and verify
- Backend: `mvn -B -ntp verify -f backend/pom.xml`
- Frontend: `cd frontend && npm ci && npm run build`
- Add meaningful unit/integration/E2E tests for touched logic. Existing CI build alone is not proof of full integration.
- Consider 401/403, data ownership, concurrency, idempotency, timeout and migration compatibility for feature-specific changes.
- In PR: summarize implementation, tests actually executed/results, risk, migration steps, follow-up items. Do not claim tests ran if they did not.

## Scope & milestones
- MVP deadline target: 2027-01-17. Beta deadline target: 2027-02-28. These are planning targets, not permission to cut mandatory security/QA.
- Prioritize Cart → Order/Inventory → Payment/Checkout → MVP QA.
- Non-MVP features (Seller, Voucher, Search Elasticsearch, K8s) belong in unscheduled backlog unless explicitly reprioritized.
- Sprint capacity assumption: 20h/week, 32 committed hours + 8 buffer per 2-week sprint. Flag overruns early.

**Start each task by referencing its Feature ID (e.g. CART-02) and URL.**


## ChatGPT → Codex handoff (primary workflow)
- For business features, the **user and ChatGPT** collaboratively draft BA requirements with `.agents/skills/ecommerce-domain/SKILL.md` and, when changing a UI, UX requirements with `.agents/skills/ui-ux-design/SKILL.md`.
- The **user explicitly approves** both specs before affected implementation starts. Templates are at `docs/project-management/templates/BA_SPEC.md` and `UX_SPEC.md`; canonical approved documents belong under `docs/features/<FEATURE-ID>/BA_SPEC.md` and `UX_SPEC.md`. See `docs/project-management/SPEC_HANDOFF.md`.
- For an Issue that implements part of a larger feature, reuse and link its approved parent-feature specs rather than creating duplicates. UI-free backend tasks do not need a UX Spec; security/infra/CI maintenance may require neither.
- The Codex Coordinator **must read and validate approved specs and the Issue before delegating Architect/Backend/Frontend**. A document is approved only if explicitly approved by the user and the approval evidence is recorded. Do not treat a Draft, a template or an agent's guess as approval.
- If approved BR-xx / AC-xx / UX-xx conflict, scope is missing, or required specifications are absent, flag the blocker and ask for a decision; do not improvise or silently expand business scope.
- The Codex custom agents `business-analyst` and `ui-ux` remain available **only on explicit request** for read-only requirement-compliance reviews or gap checks. **Do not spawn them by default** and do not perform a new discovery round when an approved spec already exists.

## Multi-Agent engineering orchestration
- See `docs/project-management/MULTI_AGENT.md` and `.codex/agents/*.toml` for role definitions. The main Codex agent is Coordinator and custom agents are on-demand.
- **Primary delivery path:** user-approved BA Spec + UX Spec (if UI) → `architect` Technical Design → `backend` and/or `frontend` → `qa` + `security` → PR → user review.
- Use `planner` for estimates or sprint/dependency risks and `devops` for infrastructure. Pure security audits (e.g. BASE-01) can use `architect`, `qa`, `security` directly.
- Before delegation define file ownership, dependencies, expected outputs and test evidence. Parallelize independent reviews; **never parallel-edit the same file**. Do not spawn every agent for routine tasks.
- The `.codex/config.toml` declares a three-subagent concurrency setting; local Codex version/support must be verified at runtime.
- Subagents are not always-running. If delegation is unavailable, explain and apply the same checks sequentially.

## Project Skills
- Catalog: `docs/project-management/SKILLS.md`; manifests under `.agents/skills/`.
- `ecommerce-domain` and `ui-ux-design` are primarily used during **ChatGPT + user discovery/design** and optionally for a requested compliance review in Codex.
- `microservice-design`, `spring-boot-development`, `vue-development`, `testing-strategy`, `security-review`, `infrastructure` support delivery tasks, loaded only as relevant.
- Architect derives technical contracts from approved requirements. Frontend implements approved UI interactions and states. QA maps actual evidence to AC-xx, BR-xx and UX-xx; Security reviews trust boundaries and ownership.
- Skill presence never proves execution, review, test pass or user approval. Report actual evidence honestly.
