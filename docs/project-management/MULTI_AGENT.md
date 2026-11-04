# Multi-Agent Workflow — Shopee Clone (Codex)

> `.codex/agents/*.toml` defines **on-demand custom subagents**. `AGENTS.md` supplies shared rules; `.agents/skills/*/SKILL.md` defines reusable procedures. The main Codex session is the Coordinator; definitions are not always-on agents.

## Roles — 9 custom subagents + 1 main coordinator

| Role | Custom agent | Core responsibility | Default editing permission |
| --- | --- | --- | --- |
| Coordinator | Main Codex session | Choose delegation, integrate findings, prepare PR for human review | Integration only |
| Business Analyst (BA) | `business-analyst` | Scope, actors, customer journeys, business rules, user stories, AC and edge cases | Read-only |
| UI/UX | `ui-ux` | User flows, interaction/state design, wireframes, responsive and accessible UX specs | Read-only by default |
| Planner | `planner` | Estimates, sprint capacity, milestones, dependencies and deadline risk | Read-only |
| Architect | `architect` | API contracts, service ownership, data flow, reliability and technical design | Read-only by default |
| Backend | `backend` | Spring Boot APIs, persistence, migrations, backend tests | Assigned files only |
| Frontend | `frontend` | Vue/Pinia pages, API integration and UI tests | Assigned files only |
| QA | `qa` | Independent test strategy, AC evidence, functional/reliability regression | Read-only by default |
| Security | `security` | JWT/RBAC, trust boundary, spoofing, IDOR, secrets and privilege review | Read-only by default |
| DevOps | `devops` | Docker/Compose, CI, Kafka, ELK, recovery tests | Assigned files only |

## Domain workflow: BA → UI/UX + Architect → Frontend

For new commerce behavior (Catalog, Cart, Checkout, Order, Inventory, Payment, Review, Seller, Shipping, Returns):

1. **Coordinator → BA:** Read Issue and `ecommerce-domain` Skill; write BA Spec with scope, actors, business rules (BR-xx), user journeys, AC, edge cases and open decisions. Mark proposed rules as proposals, *not approved requirements*.
2. **Human decision gate:** Ask the user to resolve blocking business-policy decisions. Do not silently invent Shopee marketplace policies or expand MVP to multi-vendor.
3. **Coordinator → UI/UX (if user-facing):** Translate approved BA rules into user flows, screen inventory, interaction and UI states, responsive/accessibility requirements and UX acceptance criteria, using `ui-ux-design`. Review UX choices with user before implementation. No UI/UX stage required for pure backend or infrastructure work.
4. **Coordinator → Architect:** Translate approved business requirements into service boundaries, API/data contracts, consistency/reliability and error handling using `microservice-design`; coordinate any UX-driven API/data needs.
5. **Planner:** Re-estimate/schedule after BA scope is clear; flag any sprint over 32 planned hours.
6. **Implementers:** Delegate to `backend` / `frontend` / `devops` only as relevant. Assign distinct files; don't parallel-edit overlapping files.
7. **QA + Security:** Review the resulting implementation and acceptance evidence independently using matching Skills.
8. **Coordinator:** Consolidate, link Issue, create PR; **no merge or closing Issues without human review and DoD verification**.

For pure infrastructure/CI or baseline security audits where no new domain rule is requested, bypass BA and use only the relevant specialists.

## Example delegation flows

- **BASE-01:** Coordinator → architect + qa + security in parallel (read-only) → findings and test matrix. BA not needed for technical-security baseline.
- **CART-01 (design):** Coordinator → business-analyst (`ecommerce-domain`) → human confirms cart price/stock policy → ui-ux (`ui-ux-design`) for Cart flows + architect (`microservice-design`) for service/data contracts → planner/qa review.
- **CART-02 (implementation):** Coordinator → backend (`spring-boot-development`) → qa (`testing-strategy`) and security (`security-review`) → PR → human review. Reuse approved CART-01 BA and UX specs where applicable.
- **OPS-01 (Kafka):** Coordinator → architect → devops (`infrastructure`) → qa failure tests → PR.

Subagents are bounded to **three concurrent threads** through `.codex/config.toml`, excluding Coordinator. Nine definitions does not mean nine are always running. Prefer sequential handoff when business rules need approval or implementation files overlap.

## Start prompts (send in VS Code Codex)

**BA discovery for Cart**

```text
Phân tích Issue CART-01 theo AGENTS.md / WORKFLOW.md.
Spawn subagent business-analyst, yêu cầu đọc ecommerce-domain Skill.
Viết BA Spec: actors, User Stories, Business Rules (BR-xx), edge cases,
Acceptance Criteria và câu hỏi cần tôi phê duyệt.
Chưa gọi Backend/Frontend hoặc viết code. Architect chỉ thiết kế sau khi BA rules được duyệt.
```

**Technical audit**

```text
Audit BASE-01 với các subagents architect, qa, security (read-only)
và skill tương ứng. Ghi bằng chứng verified vs suspected, không sửa code.
```

## Local verification

1. Pull the updated feature branch to the local `shopee` folder and start a **new** Codex session in that folder.
2. Confirm `.codex/agents/business-analyst.toml`, `.codex/agents/ui-ux.toml` and corresponding `.agents/skills/{ecommerce-domain,ui-ux-design}/SKILL.md` files exist.
3. Ask Coordinator to explicitly spawn `business-analyst` for BA rules and `ui-ux` for user-facing flows; request that they name the Skills read and provide BA/UX Specs.
4. Check Codex subagent thread/activity UI (or CLI `/subagents` if supported). A TOML/Skill file by itself does not prove invocation.
5. If agents/skills are not discoverable, verify project root, client/version, project trust and current Codex configuration.

See `SKILLS.md` for the role-to-Skill mapping, and `WORKFLOW.md` for Done/Review rules.

## UI/UX design handoff
- BA owns approved **business behavior**; UI/UX owns **interaction flows, interface hierarchy, screen states and usability**; Architect owns **technical contracts and data consistency**; Frontend owns **Vue implementation**.
- For user-facing features, UI/UX should inspect existing frontend views/components before proposing new UI. Document desktop/mobile, loading/empty/error/success/401/403/404 and accessibility considerations.
- UI/UX and Architect may work in parallel **after** the BA policy gate if their outputs are independent; reconcile contract needs before frontend begins.
- In a design-only Issue, ask Coordinator: `Spawn ui-ux, load ui-ux-design, propose a UI/UX Spec for CART-04 based on approved BA AC; do not write code.`
