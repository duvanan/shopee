---
name: vue-development
description: "Use when implementing Shopee Clone frontend screens or UI flows in Vue 3, TypeScript, Vite, Pinia, Tailwind, Vue Router and Axios, including API integration and error states. Not for backend-only changes."
---

# Shopee Clone — Vue Development

## Trigger
Use when a GitHub Issue changes a user-facing page, component, route, Pinia state, form, UI validation or API client. Do not use for Java-only or infrastructure-only changes.

## First read
- Issue and AC; `docs/project-management/WORKFLOW.md`.
- `frontend/src/router/index.ts`, `frontend/src/stores/auth.ts`, `frontend/src/api/http.ts`, relevant `views/`, `components/`, `types/`.
- Backend API contract. Verify against implementation when docs are stale.

## Workflow
1. Identify actor and user journey, available routes, API endpoints and required roles/permissions.
2. Define typed request/response DTOs and API methods under `frontend/src/api/`; route all business API traffic through API Gateway.
3. Create/reuse small components and Pinia state only where shared state is needed; keep data loading and mutation behavior explicit.
4. Cover loading, empty, success, form invalid, server error, 401, 403, 404 and out-of-stock/conflict where relevant.
5. Preserve access-token handling and silent refresh through existing Axios client; never put refresh cookies into JS/localStorage. Vue route guards are UX only; server enforces authorization.
6. Implement responsive layouts, labels, keyboard accessibility and disabled/submitting states; avoid duplicated submits (checkout/payment).
7. Include test cases for navigation, validation, state reset, permission checks and multi-step workflow success/failure.

## Verify
- `cd frontend && npm ci && npm run build`
- When available, run UI/component/E2E tests for touched flow; explain if no test runner is configured.
- Verify frontend data shape against backend JSON and do not invent API fields.

## Handoff
Return changed file list, screenshots or reproducible manual steps (when UI is involved), tests/commands/results, known UX risks and dependencies. Leave issue open until AC and DoD are verified.
