# UX Spec — <FEATURE-ID>: <Feature name>

> **TEMPLATE ONLY — NOT AN APPROVED DESIGN.** Create a copy at `docs/features/<FEATURE-ID>/UX_SPEC.md` for features with customer/admin UI. Remove unused sections.

| Metadata | Value |
| --- | --- |
| Feature ID / GitHub Issue | <FEATURE-ID> / https://github.com/duvanan/shopee/issues/<number> |
| Status | Draft / Needs approval / Approved |
| Owner | Product owner (user) + ChatGPT collaboration |
| Approved by | <user / not yet approved> |
| Approval reference / date | <explicit decision link / date / not yet approved> |
| BA Spec | `docs/features/<FEATURE-ID>/BA_SPEC.md` |
| Version | 0.1 |

## 1. UX goal, users and context
- Target actors / devices:
- Primary job-to-be-done and success criteria:
- In-scope screens / routes:
- Existing UI elements to reuse (confirm against frontend code):

## 2. User flows
- Primary flow: Entry → Step 1 → Step 2 → Success
- Alternative flow: validation / authorization / stale state / timeout → recovery
- Navigation, back/forward, unsaved changes, cancel behavior:

## 3. Screen inventory and low-fidelity wireframes
| Screen ID | Screen and route | Information hierarchy / primary CTA | Desktop layout | Mobile layout |
| --- | --- | --- | --- | --- |
| UX-S01 | <name> | <priority and CTA> | <outline> | <outline> |

Add ASCII wireframes or link to a reviewed design artifact when useful; do not claim a Figma file exists unless verified.

## 4. Interaction and UI state matrix
| Screen / component | Trigger | Loading/empty/success/error states | Feedback / recovery | Related BA rule |
| --- | --- | --- | --- | --- |
| <example> | <action> | <states> | <action> | BR-xx |

Cover 401, 403, 404, unavailable product, changed price/stock and duplicate-submit prevention when relevant.

## 5. Content, forms and accessibility
- Form fields, labels, validation text and error copy:
- Focus order, keyboard interaction, visible focus, semantic labels/dialog behavior:
- Contrast/non-color-only communication and screen-reader status:
- Responsive breakpoints/layout changes, overflow and mobile touch targets:

## 6. Design system / component handoff
- Existing Vue/Tailwind components to reuse:
- Proposed new components:
- API data necessary to render UI (coordinate with Architect; no invented contract):
- Visual assets / references and provenance:

## 7. UX acceptance criteria
- UX-01: **Given** <context>, **When** <action>, **Then** <observable accessible UI outcome>.
- UX-02:

## 8. Open decisions, assumptions and risks
| ID | Decision / concern | Owner (User/BA/UI-UX/Architect) | Blocking? | Resolution |
| --- | --- | --- | --- | --- |
| U-01 | <question> | <owner> | Yes / No | Pending |

## 9. Approval record (human authority)
- Decision: **Pending** / Approved / Changes requested
- Approver/date and approval reference:
- Approved screen IDs, UX criteria and exceptions:

**Important:** UI/UX must not change approved commerce policy. Any unresolved business rule goes back to the user/BA discussion; Codex must not auto-approve or silently replace this spec.

## 10. Codex handoff
- Frontend: pages/components/states/validation/responsive implementation notes:
- QA: UX-xx test scenarios:
- Architect: data needs and feasibility questions:
