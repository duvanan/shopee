# ChatGPT → Codex: BA/UX Spec Handoff (Shopee Clone)

## Why this process exists
Business and UX decisions belong to **the user + ChatGPT collaboration**. The Codex Coordinator consumes already-reviewed specs; it does **not** restart discovery or ask `business-analyst` / `ui-ux` to invent requirements for every Issue.

- Business workflow Skill: `.agents/skills/ecommerce-domain/SKILL.md`
- UX workflow Skill: `.agents/skills/ui-ux-design/SKILL.md`
- Reusable templates: `docs/project-management/templates/BA_SPEC.md` and `UX_SPEC.md`
- Approved feature documents: `docs/features/<FEATURE-ID>/BA_SPEC.md` and `UX_SPEC.md` (UX only for UI work).
- Technical design created by Codex Architect: `docs/features/<FEATURE-ID>/TECHNICAL_DESIGN.md`.

## Process

1. **User + ChatGPT — BA:** Discuss domain assumptions, happy/error journeys, BR-xx, AC-xx, dependencies and explicit out-of-scope items. Draft a feature-specific BA Spec from the template.
2. **User + ChatGPT — UI/UX (when UI work):** Based on approved BA rules, draft UX Spec with flows, screen inventory/wireframes, states, responsive/accessibility, UX-xx criteria.
3. **Explicit approval:** The user reviews decisions and authorizes the spec. Set `Status: Approved` **only after** explicit approval. Record approver, date and reference in each spec. Do not treat ChatGPT agreement alone as user approval.
4. **Commit/share with Codex:** Put approved specs in `docs/features/<FEATURE-ID>/` on a branch Codex can read (or supply a clear attached spec). Link the Issue to the canonical versions. If branch/PR spec and Issue conflict, stop and ask which is authoritative.
5. **Codex Coordinator — preflight:** Read Issue + `AGENTS.md` + `WORKFLOW.md` + BA Spec and UX Spec (for UI issues). Verify status/approval evidence, IDs, scope and dependency. Do not alter approved business/UX decisions silently.
6. **Architect:** Produce Technical Design (API contract, data ownership, transactions, consistency, authorization and failure modes), using `microservice-design` and approved BA/UX constraints. **Escalate** any infeasible or conflicting requirement; do not auto-approve substitutes.
7. **Implementers:** Backend and/or Frontend implement approved BR/AC/UX-xx in separate file ownership where parallel work is useful.
8. **QA + Security:** Check evidence for AC/UX-xx; report gaps separately from test results. Coordinator prepares PR for **human review**, never auto-merges.

## Spec status / missing-spec behavior

- **Approved:** proceed only within the approved Issue scope.
- **Draft / Needs approval:** do not start affected production implementation; surface blocking decisions and ask the user.
- **Missing BA Spec on a new business-facing feature:** do not infer requirements from roadmap or broad Issue title. Request the spec or confirmation of a documented existing approved source.
- **Missing UX Spec on a task that changes customer/admin interface:** request UX handoff before building new UI. If the task is truly backend-only, UX Spec may be N/A.
- **Tech-only maintenance, urgent security fixes, infra/CI/ELK:** BA/UX specs are normally N/A; follow Issue acceptance criteria and technical checks.
- **Existing feature split across Issues:** an earlier approved BA/UX Spec may be reused by linking it and checking its version. Do not demand duplicate specs per small implementation task.

## Optional reviewer agents (not default discovery)
The `business-analyst` and `ui-ux` custom Codex agents remain available for **explicitly requested** read-only gap/compliance reviews:
- `business-analyst`: compare Issue, BA Spec, implementation behavior and BR/AC; report deviations/questions; do not rewrite approval history.
- `ui-ux`: compare UX Spec with frontend implementation, responsive and accessibility behavior; report missing UI states and UX-xx gaps.

Neither agent is spawned by default on routine feature implementation, and neither can approve or amend a spec on behalf of the user.

## Example prompt to Codex
```text
Implement Issue CART-02 using the approved specification for CART-01
under docs/features/CART-01/BA_SPEC.md (and UX_SPEC.md if the Issue modifies UI).
Do not run a new BA/UI-UX discovery pass.
Verify the spec is Approved and linked to the Issue, then delegate Architect
for Technical Design and Backend/Frontend as scoped, followed by QA/Security.
If a business policy, UX requirement or spec status is missing or conflicting,
stop that scope and ask me instead of guessing. Do not merge PR.
```

**Templates are never evidence of approval.** Actual feature specs must be filled in and explicitly approved by the user.
