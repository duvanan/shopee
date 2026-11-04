---
name: ui-ux-design
description: "Use to design or review Shopee Clone frontend user journeys, page layouts, responsive interactions, usability, accessibility and UI states for customer checkout/cart/catalog and admin screens before Vue implementation."
---

# UI/UX Design — Shopee Clone

## When to use
Use this Skill when a user-facing task adds or revises a screen, navigation, form, flow, interaction pattern, responsive layout or accessibility behavior. It can also guide a focused UI/UX review of existing pages.

Do not invoke on pure backend, security, Kafka/ELK or CI tasks without user-visible experience impact. **Primary workflow:** user + ChatGPT design and review the UX Spec before Codex engineering. The `ui-ux` Codex subagent is **optional**, used only for explicit user-requested compliance/gap reviews or explicitly requested Codex discovery. A Skill file is reusable guidance, not an autonomous agent.

## Start from project context
1. Read the GitHub Issue and its approved BA spec/user stories/AC if the change has business behavior. No approved spec means unresolved business decisions must be flagged, not invented.
2. Inspect existing `frontend/src/layouts`, `frontend/src/views`, `frontend/src/components`, `frontend/src/style.css`, `frontend/tailwind.config.js`, `frontend/src/router`, relevant mocks in `docs/mockups` and typed API shapes.
3. Look for current shared patterns: product cards, auth forms, admin lists/forms, navigation, pagination, confirmations, loading and error feedback. Prefer consistency/reuse over redesign.
4. Identify target users (Guest, Customer, Admin; Seller only if explicitly approved), device/context, key actions and dependencies.

## Design workflow
1. **Jobs & journeys:** state the user goal, trigger, major steps, success condition and points of confusion. Keep business rules owned by BA.
2. **Information architecture:** list the required pages, sections, hierarchy, navigation and primary/secondary calls to action. Avoid adding unapproved features.
3. **Flows:** map happy path and alternatives (invalid input, auth required, price/stock changed, request timeout, payment pending, duplicate click, missing resource).
4. **Wireframes:** provide low-fidelity screen descriptions or ASCII wireframes with labels and hierarchy; do not claim a pixel-perfect artifact or generate assets unless explicitly requested. Show desktop/mobile distinctions.
5. **Interaction specification:** define field labels, form errors, confirm dialogs, toast/inline feedback, keyboard/submit behavior, focus management, pagination/search and irreversible action confirmation.
6. **State inventory:** enumerate initial, loading/skeleton, empty, partial data, success, failed, 401, 403, 404, stale data and offline/network failure when relevant. Explicitly describe what the user sees and can do next.
7. **Responsive design:** mobile-first layout, touch sizes, tap targets, overflow, readable product info, form and table alternatives; verify desktop continuity.
8. **Accessibility:** semantic controls/heading structure, keyboard and focus order, visible focus, field-level errors with associated labels, accessible dialog behavior, contrast and non-color-only statuses. Prefer WCAG 2.2 AA principles as design goals; do not claim compliance without verification.
9. **Design handoff:** map screens to existing Vue components or proposed reusable components, typed API needs, UX acceptance criteria, QA scenarios and outstanding BA/architect questions.

## Domain-specific UX checkpoints
| Area | Important scenarios |
| --- | --- |
| Catalog & search | Search/filter feedback, pagination, empty results, missing photos, prices and stock display |
| Cart | Quantity bounds, removed/unavailable product, price updated, login boundary, cart empty |
| Checkout/payment | Clear totals before confirm, shipping information, disabled duplicate submission, pending/failed/success results, retry without double charge |
| Orders | Status history, understandable labels, ownership/privacy, cancellation policy only if BA approves |
| Reviews | Login-required state, rating selection with keyboard, successful submission and moderation errors |
| Admin | Forms with validation, data tables responsive behavior, destructive confirmation, role-based affordances |

## Spec handoff and approval
- Start from `docs/project-management/templates/UX_SPEC.md` and create `docs/features/<FEATURE-ID>/UX_SPEC.md` only for features with customer/admin UI changes. The corresponding BA Spec must be approved first for business behavior.
- Record `Status: Approved`, explicit user approval reference and version only after **human approval**, never based on an AI inference.
- Codex Frontend consumes the approved UX Spec and maps implementation to UX-xx acceptance criteria; Codex does **not** restart default UI/UX discovery. See `docs/project-management/SPEC_HANDOFF.md`.
- When requested as an optional Codex reviewer, compare actual UI with approved flows, states, responsive and accessibility criteria and report evidence rather than inventing a new design.

## Required handoff format

```markdown
# UI/UX Spec — <FEATURE-ID>: <Feature>
Status: Draft / Needs approval / Approved (only after explicit review)
Sources: Issue, BA Spec, existing frontend files

## Users and UX goal
Actors, core task, user success criteria

## User flows
Primary journey, alternative paths, recovery states

## Screen inventory and wireframes
Each screen: hierarchy, CTA, responsive behavior

## UI states and interactions
Field validation, loading, empty, success, error, auth/permission, confirmation

## Design system and components
Existing component reuse, new components proposed, copy and accessibility

## UX acceptance criteria
- UX-01: Given ... When ... Then ...
- UX-02: ...

## Open decisions
Business policies for BA/user, technical feasibility for architect/frontend

## Handoff
Frontend: screen/component/state mapping
QA: test cases for each UX-xx
Planner: scope/estimate implications
```

## Definition of Ready for frontend handoff
- BA business rules relevant to UI are approved, or unresolved decisions explicitly blocked.
- Every screen has major states and navigation/CTA outcomes.
- Error/validation copy and recovery action are defined.
- Mobile and desktop behavior are described; accessibility checks are included.
- UX acceptance criteria map to testable behavior; design is reviewed before frontend coding.

## Non-goals
- Do not modify source code unless explicitly delegated.
- Do not claim a working Figma file, polished mockup or verified WCAG compliance from text-only specs.
- Do not decide payment, stock, cancellation, seller or refund policies; those belong to BA and user approval.
- Do not treat backend API implementation choices as user experience requirements without checking contracts.
