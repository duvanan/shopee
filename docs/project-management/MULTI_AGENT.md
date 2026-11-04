# Multi-Agent workflow for Shopee Clone (Codex)

> Added for the local Codex app / CLI / IDE. Files in `.codex/agents/` are **custom agent definitions**, not always-running processes. Actual subagent spawning depends on the Codex session and explicit delegation. Root `AGENTS.md` remains the shared repository policy.

## Participants

| Role | Agent name | Responsibility | Editing policy |
| --- | --- | --- | --- |
| Coordinator | Main Codex session | Read Issue, decide delegation, merge findings, ask user to review PR | Own integration |
| Planning | `planner` | Scope/estimate, deadline, dependencies and sprint risks | Analysis only |
| Architecture | `architect` | API/DB boundaries, consistency, technical design | Analysis by default |
| Backend | `backend` | Spring Boot service implementation and backend tests | Assigned files only |
| Frontend | `frontend` | Vue/Pinia/UI/API integration | Assigned files only |
| QA | `qa` | Independent test matrix, test execution and evidence | Analysis by default |
| Security | `security` | JWT/RBAC/Gateway/IDOR/trust-boundary review | Analysis by default |
| DevOps | `devops` | Compose, CI, Kafka, ELK, health and recovery | Assigned files only |

## Delegation policy
1. Main agent reads the GitHub Issue and `WORKFLOW.md`; calls `planner` when scope, estimate or deadline is uncertain.
2. For service/data/API changes, delegate architecture to `architect` before coding.
3. Implementers `backend`, `frontend`, `devops` work only on independently owned files; prefer sequential execution if their files overlap.
4. `qa` and `security` review independently of implementation; they report evidence and blockers.
5. Coordinator synthesizes results, checks DoD, creates PR and **waits for human approval** before merge/Done.
6. Do not spawn all roles on every Issue. For simple docs issues, coordinator alone is sufficient; complex changes use 2–3 suitable subagents.
7. Treat concurrency as 3 max here; independent exploratory reviews may run in parallel, conflicting writes must be sequential.

## Typical flows

### Feature: CART-02
`Coordinator → architect (Redis/API + service boundaries) → backend (Cart APIs) → qa + security (review/test) → PR → human review`.
For CART-04 add `frontend` after API contract is stable.

### Feature: OPS-01
`Coordinator → architect (replication/failover design) → devops (3-node KRaft) → qa (fault test evidence) → PR`.

### Baseline: BASE-01
`Coordinator → (qa, security, architect) in parallel read-only → consolidate issue findings → PR/docs`.

## Start task prompt (send in Codex)
```text
Hãy thực hiện Issue #1 (BASE-01) trong duvanan/shopee theo AGENTS.md và WORKFLOW.md.
Dùng custom subagents qa, security, architect để audit độc lập theo phạm vi.
Main agent tổng hợp, đưa ra test matrix và tạo branch/PR phù hợp; không merge, không đóng Issue nếu chưa được tôi review.
```

## Verify after merging the workflow PR
- Pull latest `main`; restart Codex session to refresh project AGENTS/config instructions.
- Check `.codex/config.toml` and `.codex/agents/{role}.toml` are present.
- Ask explicitly for subagent delegation; inspect active threads (CLI `/subagents` when supported).
- If custom agents do not appear, verify Codex version, trusted-project config and current agent availability.
- A TOML file by itself does **not** launch an autonomous always-on agent; local project rules are not a scheduler.

References: https://learn.chatgpt.com/docs/agent-configuration/subagents and https://learn.chatgpt.com/docs/config-file/config-reference

## Shared Skills
The seven custom agent roles reuse six instruction-only project Skills located under `.agents/skills/`. See `docs/project-management/SKILLS.md` for the complete role-to-Skill matrix, invocation examples and runtime verification checklist.

- `architect` → `microservice-design` (+ `security-review` if trust-sensitive).
- `backend` → `spring-boot-development` (+ design and testing when needed).
- `frontend` → `vue-development` (+ `testing-strategy`).
- `qa` → `testing-strategy`.
- `security` → `security-review`.
- `devops` → `infrastructure` (+ design/testing when needed).
- `planner` → existing `WORKFLOW.md` / `ROADMAP_2026_2027.md`; no custom Skill required.

A Skill never launches an agent by itself. Coordinator chooses subagents and each specialist loads only matching Skills as needed.
