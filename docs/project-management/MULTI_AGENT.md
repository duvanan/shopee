# Multi-Agent Engineering Workflow — Shopee Clone

This repository contains **nine optional Codex specialist definitions** and **eight reusable Skills**. The user and ChatGPT are the **product discovery / UX authority**; Codex is the **engineering delivery system**.

Read `AGENTS.md`, `docs/project-management/WORKFLOW.md` and `docs/project-management/SPEC_HANDOFF.md` before implementing a feature.

## Default flow

```text
User + ChatGPT
  |-- ecommerce-domain → BA_SPEC.md (business rules and AC)
  |-- ui-ux-design → UX_SPEC.md (UI flows and UX AC, when needed)
  |-- explicit human approval of specs
  v
Codex Coordinator (read Issue + approved specs)
  v
Architect (TECHNICAL_DESIGN.md: APIs/data/consistency/security)
  v
Backend / Frontend (assigned files only; coordinate API contracts)
  v
QA + Security (test evidence / independent review)
  v
Pull Request → User review → User-authorized merge
```

The Coordinator must **not** automatically run fresh BA/UI-UX discovery for every Issue. An approved parent-feature spec may cover multiple implementation Issues.

## Agent responsibilities

| Custom agent | Default role | Default on business feature? |
| --- | --- | --- |
| `planner` | Scope/dependency/estimate/sprint risk using approved requirements | Only if planning needed |
| `architect` | Technical Design, API/service boundaries, reliability/security | **Yes**, for architecture-impacting changes |
| `backend` | Implement scoped Spring Boot APIs, migrations and tests | When backend changes |
| `frontend` | Implement approved UX Spec with Vue/TypeScript/Pinia | When frontend changes |
| `qa` | Review AC/UX-xx coverage; execute/report tests and defects | For changed behavior |
| `security` | Review authorization, session, spoofing, IDOR, secrets | For security-sensitive changes |
| `devops` | Compose, CI, Kafka, ELK and recovery tasks | When infrastructure changes |
| `business-analyst` | **Optional read-only review** of implementation vs approved BA Spec (not default author of requirements) | **No**, unless user requests |
| `ui-ux` | **Optional read-only UX compliance review** vs approved UX Spec (not default discovery) | **No**, unless user requests |

The **main Codex session** is Coordinator, not an additional custom TOML role. Existing concurrency is configured as three subagents; the coordinator selects only relevant agents, never all nine at once.

## Required preflight (Coordinator)

1. Resolve `<FEATURE-ID>` and GitHub Issue, scope, dependencies, acceptance criteria.
2. For business behavior, locate `docs/features/<FEATURE-ID>/BA_SPEC.md` or an explicit approved parent spec link. For customer/admin UI changes locate `UX_SPEC.md`; if UI does not change mark UX N/A.
3. Read **approval status and evidence**. A template, draft or undocumented assertion of approval is insufficient. If required specs are missing or inconsistent, stop affected implementation and ask the user. Do not fabricate approval.
4. Ask `architect` for `docs/features/<FEATURE-ID>/TECHNICAL_DESIGN.md` when design is needed. Technical design must trace back to BA/UX AC and flag conflicts rather than overwrite them.
5. Delegate only appropriate implementation and reviewers. QA maps tests to `BR-xx`, `AC-xx`, `UX-xx`; Security checks relevant risks; report commands/results and unresolved gaps.
6. Create a PR and request human review. Never auto-merge, auto-close or silently extend sprint scope.

## Optional BA and UI/UX review agents

Use **only when specifically requested by the user**, such as:
- `business-analyst` + `ecommerce-domain`: check an existing implementation/Issue for deviations from approved business rules; produce a gap report, **no new policy**.
- `ui-ux` + `ui-ux-design`: inspect frontend for differences from approved user flows, states, responsiveness/accessibility criteria; produce a gap report, **no new visual mandate**.

If user explicitly requests fresh discovery *inside Codex*, BA/UI-UX agents may draft proposals, but may not claim user approval. The default remains **ChatGPT + user** discovery/design.

## Examples

- **BASE-01 technical/security audit:** Coordinator → architect + qa + security in parallel (read-only). Neither BA nor UI/UX is required.
- **CART-01:** user + ChatGPT draft BA Spec and UX Spec for cart → user approves → Codex architect designs Redis/API and dependencies.
- **CART-02:** Coordinator checks approved CART-01 BA Spec → architect only if design is unresolved → backend → qa/security → PR.
- **CART-04:** Coordinator checks approved CART-01 BA + UX Specs → frontend → qa/security → PR.
- **OPS-01:** Coordinator → architect → devops + qa for Kafka; BA/UX not required.

## Starter prompt for VS Code Codex

```text
Implement Issue CART-02 according to approved BA/UX specs in
docs/features/CART-01/ (reuse the parent feature specs).
Do not spawn business-analyst or ui-ux for new discovery by default.
First verify Status: Approved and record of my explicit approval.
Delegate Architect for Technical Design as necessary, Backend for implementation,
then QA and Security for review. Stop and raise blocking spec conflicts;
do not invent new business rules, close the Issue or merge PR.
```

## Verify locally

Pull the latest workflow branch, restart the Codex session, check `.codex/agents/`, `.agents/skills/` and the spec templates. Custom-agent/Skill files alone do not launch a process or prove a review occurred.
