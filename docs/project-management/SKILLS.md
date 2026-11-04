# Shopee Clone — Project Skill Catalog

**Eight reusable Codex Skills** live under `.agents/skills/<skill-name>/SKILL.md`. Each uses YAML frontmatter with `name` and `description`. A Skill is a reusable workflow, not a separate agent or an always-running process.

## Role → Skill mapping

| Role | Primary Skill / reference | Additional Skill when relevant |
| --- | --- | --- |
| Main Coordinator | `AGENTS.md`, `WORKFLOW.md` and assigned Issue | Delegates specialists based on scope |
| `business-analyst` | `ecommerce-domain` | None required |
| `ui-ux` | `ui-ux-design` | Approved BA Spec and existing frontend screens |
| `planner` | `WORKFLOW.md`, `ROADMAP_2026_2027.md` | Review BA Spec before final estimates |
| `architect` | `microservice-design` | BA Spec and `ecommerce-domain` for business context, `security-review` for auth |
| `backend` | `spring-boot-development` | `microservice-design`, `testing-strategy` |
| `frontend` | `vue-development` | Approved UI/UX Spec, `ui-ux-design` for reference, `testing-strategy` |
| `qa` | `testing-strategy` | BA AC/business rules and `security-review` where appropriate |
| `security` | `security-review` | `testing-strategy` for regression |
| `devops` | `infrastructure` | `microservice-design`, `testing-strategy` |

## When each Skill applies

- **`ecommerce-domain`**: user journeys, actors, order/cart/stock/payment business rules, exception cases, Gherkin AC, multi-seller vs MVP scope. BA uses before technical design for business-facing features.
- **`ui-ux-design`**: user flows, screen hierarchy, interaction specs, empty/loading/error/success states, responsive layouts, accessible patterns and UX acceptance criteria for customer/admin screens.
- **`microservice-design`**: service boundaries, API/data contracts, distributed consistency and failure scenarios.
- **`spring-boot-development`**: Java/Spring implementation, validation, Flyway, Redis/Kafka and backend tests.
- **`vue-development`**: Vue/Pinia/Router, typed API integration, responsive UI and error states.
- **`security-review`**: RBAC/JWT/session/ownership, Gateway trust boundaries and secrets.
- **`testing-strategy`**: unit/integration/E2E, test evidence, reliability and negative-path matrix.
- **`infrastructure`**: Docker/CI, Kafka 3-node KRaft, Filebeat → Kafka → Logstash → Elasticsearch/Kibana and recovery.

## Usage example: CART-01

1. Coordinator delegates `business-analyst`, explicitly loading `ecommerce-domain` and the Issue.
2. BA delivers a **draft** with BR/AC plus unresolved decisions (e.g. product price changed while in cart; out-of-stock behavior).
3. User approves business-policy choices. BA cannot approve them on the user's behalf.
4. UI/UX designs Cart screen flow/states with `ui-ux-design`; Architect designs API/Redis contracts and concurrency from approved business behavior. Reconcile any data required by the interface before frontend.
5. Planner checks estimate, dependencies and deadline; QA maps AC to test scenarios.

For **BASE-01** security audit, BA is not required; use Architect + QA + Security.

## Local check

1. Pull current branch and start a fresh Codex session at repository root.
2. Check the eight Skill files under `.agents/skills/` and nine agent TOMLs under `.codex/agents/`.
3. Ask for an explicit `business-analyst` run with `ecommerce-domain`, then an `ui-ux` run with `ui-ux-design` on a user-facing feature; verify both actually load the Skills and produce BA/UX Specs.
4. Avoid assuming agent launch, passing tests or approved rules solely because files exist.

## Acceptance tracking

- [x] Eight Skill manifests committed on workflow branch.
- [x] Nine custom agent definitions available on workflow branch.
- [x] Coordinator/Architect/Planner/QA/Frontend documented BA-first and UI/UX design handoff for user-facing commerce features.
- [ ] Pull updated branch into local repo and confirm Codex discovers the new agent/Skill.
- [ ] Validate a real BA Spec and UI/UX Spec against a GitHub Issue and resolve blocking decisions with the user.

See `MULTI_AGENT.md` and `WORKFLOW.md` for orchestration and Definition of Ready/Done.
