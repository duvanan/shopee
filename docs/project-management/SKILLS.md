# Shopee Clone — Skill Catalog (Hybrid ChatGPT + Codex)

The project has **eight reusable Skill manifests** under `.agents/skills/`. **Skills are workflows, not always-running agents.** User-approved feature specs are the contract for engineering. See `SPEC_HANDOFF.md` and the templates in `docs/project-management/templates/`.

## Primary ownership

| Skill | Main use | Users / agents |
| --- | --- | --- |
| `ecommerce-domain` | BA discovery, business rules, AC, edge cases and domain glossary | **User + ChatGPT**; optional Codex BA compliance review |
| `ui-ux-design` | User flows, screen/wireframe outlines, UI states, responsive/a11y and UX criteria | **User + ChatGPT**; optional Codex UI/UX compliance review |
| `microservice-design` | Backend/system Technical Design, service boundaries and consistency | Codex `architect` |
| `spring-boot-development` | Backend implementation and tests | Codex `backend` |
| `vue-development` | Vue frontend implementation of approved UX Spec | Codex `frontend` |
| `testing-strategy` | AC/UX acceptance, unit/integration/E2E/failure testing | Codex `qa`, implementers as relevant |
| `security-review` | JWT/RBAC, user ownership, Gateway and trust-boundary review | Codex `security` and relevant implementers |
| `infrastructure` | Docker/CI/Kafka/ELK and recovery | Codex `devops` |

`planner` uses the workflow/roadmap and approved specs to evaluate estimates and dependencies. No separate planning Skill needed.

## Contract between phases

1. **User + ChatGPT** prepare a feature BA Spec from `templates/BA_SPEC.md` using `ecommerce-domain`.
2. For customer/admin UI work, **User + ChatGPT** prepare UX Spec from `templates/UX_SPEC.md` using `ui-ux-design`.
3. The **user explicitly approves** each required spec. The version, status, approval reference and decision owner are recorded in the canonical feature files.
4. **Codex Coordinator** reads approved specs, assigns Architect → Backend/Frontend → QA/Security. Codex doesn't silently redo BA/UX.
5. `business-analyst` and `ui-ux` Codex roles are retained for **optional user-requested read-only compliance reviews**, not default delegation.
6. For backend-only, infra or technical security tasks, mark irrelevant specs N/A and use existing Issue AC or approved linked parent spec.

**No Skill, template or agent may approve requirements on the user's behalf.** If a required spec is absent, Draft, contradictory or ambiguous, stop affected scope and request a decision.

## Verify after pulling the branch

- Confirm **eight** `.agents/skills/*/SKILL.md` manifests and **nine** `.codex/agents/*.toml` definitions are present.
- Confirm `SPEC_HANDOFF.md`, `templates/BA_SPEC.md`, `templates/UX_SPEC.md` exist.
- In a new Codex session, ask the Coordinator to summarize **which approved specs it read** and which specialist agents it actually spawned.
- Use a real, user-approved feature spec to test the end-to-end handoff; templates alone are not approved.
