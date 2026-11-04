# BA Spec — <FEATURE-ID>: <Feature name>

> **TEMPLATE ONLY — NOT APPROVED REQUIREMENTS.** Create a copy at `docs/features/<FEATURE-ID>/BA_SPEC.md` for each business feature. Replace all placeholders and remove unused sections.

| Metadata | Value |
| --- | --- |
| Feature ID / GitHub Issue | <FEATURE-ID> / https://github.com/duvanan/shopee/issues/<number> |
| Status | Draft / Needs approval / Approved |
| Owner | Product owner (user) + ChatGPT collaboration |
| Approved by | <user / not yet approved> |
| Approval reference / date | <explicit decision link / date / not yet approved> |
| Related UX Spec | `docs/features/<FEATURE-ID>/UX_SPEC.md` or N/A |
| Version | 0.1 |

## 1. Problem and business outcome
- Problem:
- Why now / business success measure:
- Constraints and assumptions (mark unverified assumptions):

## 2. Actors and scope
- Actors (Guest / Customer / Admin / Seller only if approved):
- In scope:
- Out of scope:
- Dependencies on existing system and other Issues:
- MVP vs future marketplace expansion:

## 3. User stories and journeys
- US-01: As a <actor>, I want <action> so that <benefit>.
- Happy path (numbered steps):
- Alternative/error paths:

## 4. Business rules (authoritative after approval)
| Rule ID | Rule | Source / decision | Status |
| --- | --- | --- | --- |
| BR-01 | <testable invariant / eligibility / lifecycle rule> | <discussion or Issue> | Proposed / Approved |

## 5. State lifecycle & dependencies
- Business entities:
- Allowed status transitions:
- Invalid transitions and user-visible outcome:
- Cross-feature dependencies (Cart → Inventory → Order → Payment):

## 6. Acceptance criteria (behavior, not implementation)
- AC-01: **Given** <precondition>, **When** <action>, **Then** <observable result>.
- AC-02:

## 7. Edge cases / exceptions
| Scenario | Expected business outcome | Priority | Related AC/BR |
| --- | --- | --- | --- |
| <e.g. price changes before checkout> | <policy must be approved> | P0/P1/P2 | <ID> |

## 8. Open decisions and risks
| Decision ID | Question | Options / tradeoffs | Owner | Blocking? | Resolution |
| --- | --- | --- | --- | --- | --- |
| D-01 | <business question> | <A / B> | User | Yes / No | Pending |

## 9. Approval record (human authority)
- Decision: **Pending** / Approved / Changes requested
- Approver and date:
- Approved BR/AC IDs and any exceptions:
- Evidence of explicit approval (conversation link, Issue comment, PR review, etc.):

**Important:** Codex must not mark this spec Approved merely because it exists or because an agent wrote it. Do not invent unresolved business policies. If a breaking ambiguity remains, request a targeted decision instead of implementing a guess.

## 10. Codex handoff
- Technical questions for Architect:
- APIs / data behaviors needed (functional outcomes only):
- QA test mapping from BR/AC:
- Planner dependencies or estimate considerations:
