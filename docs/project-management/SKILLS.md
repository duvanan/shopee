# Shopee Clone — Skill Catalog & Routing

Six project-local, instruction-only Codex Skills live at `.agents/skills/<name>/SKILL.md`. Each file includes **YAML frontmatter** `name` and `description` for discovery. Skills are shared reusable workflows, not persistent agents, and are loaded only when relevant. Agent-specific guidance lives under `.codex/agents/`.

## Role → reusable Skill mapping

| Codex role | Primary Skill | Extra Skill when needed |
| --- | --- | --- |
| Coordinator (main session) | Read `AGENTS.md`, workflow and assigned Issue | Select delegated specialist skills |
| `planner` | `docs/project-management/WORKFLOW.md`, `ROADMAP_2026_2027.md` | No separate planning Skill yet |
| `architect` | `microservice-design` | `security-review` for auth/trust |
| `backend` | `spring-boot-development` | `microservice-design`, `testing-strategy` |
| `frontend` | `vue-development` | `testing-strategy` |
| `qa` | `testing-strategy` | `security-review` when relevant |
| `security` | `security-review` | `testing-strategy` for test matrix |
| `devops` | `infrastructure` | `microservice-design`, `testing-strategy` |

### When to use which Skill

- `microservice-design`: service boundaries, Cart/Order/Inventory/Payment, API/event design and consistency.
- `spring-boot-development`: Java service implementation, Flyway, Redis/Kafka integration, backend verification.
- `vue-development`: Vue 3/Pinia/Router, forms, typed Axios integration and UI states.
- `security-review`: JWT, RBAC, 401/403, role revocation, X-User-* trust boundary, IDOR and secrets.
- `testing-strategy`: test matrix, reproducible unit/integration/E2E evidence and failure-mode QA.
- `infrastructure`: Docker/CI, Kafka 3-node KRaft, Filebeat → Kafka → Logstash → Elasticsearch/Kibana and recovery.

## Example

**Issue #1 (BASE-01) — audit:** Coordinator delegates independent read-only reviews to `architect`, `qa` and `security`; they use relevant design/testing/security Skills as needed, then coordinator records findings and tests. No backend implementation subagent required.

**Issue #6 (CART-02) — Cart Backend:** `architect` uses `microservice-design` for Redis/API/failure matrix; `backend` uses `spring-boot-development` for implementation; `qa` uses `testing-strategy`. Review then PR.

**Issue #30 (OPS-02) — ELK:** `devops` uses `infrastructure`; `qa` uses `testing-strategy` to verify broker/log pipeline outages.

## How to verify locally after merge

1. `git pull` / checkout latest branch and start a new Codex session in this repository.
2. Inspect `.agents/skills/<skill-name>/SKILL.md`; verify the file begins with `---` and YAML keys `name`, `description`.
3. Ask Codex to list discoverable project Skills or reference one explicitly by name, e.g. `Use spring-boot-development for CART-02`.
4. For multi-agent work, ask coordinator to delegate and inspect subagent results; Skill discovery does not prove an agent was launched.
5. If Codex does not discover these skills, verify active working directory, supported client/version and project trust/config; refer to current Codex documentation.

## Acceptance check (repository config vs runtime)

- [x] Six `SKILL.md` manifests committed on workflow branch.
- [x] Agent TOML files reference matching Skills, if the corresponding task arises.
- [x] `AGENTS.md` and `MULTI_AGENT.md` reference Skill catalog.
- [ ] GitHub PR merged into main (pending user review).
- [ ] Codex has discovered/loaded a Skill in an actual local session.
- [ ] At least one end-to-end delegated Issue validated against AC and DoD.

**No automatic Skill invocation, agent start, test pass or production readiness is implied by adding these files.**
