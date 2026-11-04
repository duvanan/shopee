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


## Multi-Agent orchestration (project-scoped custom Codex agents)
- See `docs/project-management/MULTI_AGENT.md` and `.codex/agents/*.toml` for role definitions.
- The **main agent is coordinator**. Do not behave as a single agent doing all specialist work on complicated, cross-service tasks. Delegate suitably scoped analysis to `business-analyst`, `ui-ux`, `planner`, `architect`, `qa`, or `security`, and implementation to `backend`, `frontend`, or `devops`.
- Before delegation decide file ownership, dependencies, expected output and acceptance evidence. Parallelize independent reviews; **never parallel-edit the same file**.
- On routine changes spawn only required agents; max 3 concurrent subagents is configured in `.codex/config.toml` to control cost and merge conflicts.
- On BASE-01 prefer `architect` + `qa` + `security` for independent read-only audit. On new Cart/Order/Payment/Review business features, delegate `business-analyst` using `ecommerce-domain` to define rules and AC first, then UI/UX (for user-facing screens) and architect coordinate on design → backend/frontend → independent QA/Security review. BA owns WHAT/WHY; UI/UX owns user interaction and usability; architect owns technical HOW. For Kafka/ELK use architect → devops → QA.
- Subagents are on-demand and do not run automatically from their TOML definitions. If delegation is unavailable, explain the limitation and follow the same role-based checklists sequentially.

## Project-local reusable Skills
- Skill catalog: `docs/project-management/SKILLS.md`; manifests: `.agents/skills/{ecommerce-domain,ui-ux-design,microservice-design,spring-boot-development,vue-development,security-review,testing-strategy,infrastructure}/SKILL.md`.
- Load a matching Skill only when the assigned Issue actually calls for that workflow; Skills are **shared procedures**, not one Skill per agent and not always-on processes.
- Use `microservice-design` before implementation involving new services or distributed consistency, `testing-strategy` when preparing acceptance evidence, and `security-review` for auth/RBAC/ownership sensitive changes.
- Backend/frontend/DevOps agents should consult their respective implementation Skill, then produce the documented output and test evidence. The coordinator remains responsible for reviewing combined changes.
- **Skill presence does not prove execution**. Report what was actually checked, tested and delegated.
- For business-facing Issues, load `ecommerce-domain` in `business-analyst` first to clarify user stories, rule IDs, edge cases, AC and decisions requiring user approval. Do not let Architect or Backend invent unapproved commerce rules or change sprint scope silently.
- Planner estimates **after** BA clarifies scope; for infrastructure/security-only tasks, BA review is optional and should not delay fixes.
- When a feature adds or materially changes a customer/admin screen, delegate `ui-ux` using `ui-ux-design` after BA business rules are clear. Require a screen/flow/state/accessibility specification and UX acceptance criteria before frontend implementation.
- Do not let UI/UX invent unapproved commerce policies or silently redesign unrelated pages. Frontend follows approved UX handoff; QA maps both BA business AC and UX-xx criteria to test evidence.
