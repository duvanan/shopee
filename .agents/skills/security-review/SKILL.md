---
name: security-review
description: "Use for independent threat-focused reviews of Shopee Clone authentication, Spring Security, JWT/RBAC, Gateway authorization, user ownership, service trust boundaries or secrets. Use before accepting security-sensitive PRs."
---

# Shopee Clone — Security Review

## Trigger
Use on Auth/RBAC/User/Admin/Cart/Order/Payment changes, new public endpoints, JWT/cookie/Redis config, or Gateway/proxy exposure changes. Do not use for unrelated visual refinements; no need to run on every documentation edit.

## Read and map trust
- GitHub Issue & AC, `docs/architecture/rbac.md`.
- Gateway `JwtAuthenticationFilter`, route authorization YAML, service `SecurityConfig`/`HeaderAuthenticationFilter`, auth/refresh code, frontend `http.ts`.
- Relevant Controller/Service data access checks and local Docker/network boundaries.

## Review checklist
1. **Identity:** signatures, JWT expiry, role/permission claims, refresh rotation and cookie `httpOnly`/`Secure`/`SameSite`/`Path`; note residual risks.
2. **Authorization:** 401 vs 403, correct HTTP-method permissions, default-deny routes, stale JWT permissions after revocation, role/permission escalation.
3. **Trust boundary:** direct microservice access, spoofed `X-User-Id`/`X-User-Roles`/`X-User-Permissions`, trusted Gateway → service routing, header override/normalization. Only report a concrete exploit as confirmed after evidence.
4. **Ownership:** cross-account access (IDOR), cart isolation, order detail/history, admin actions and seller resources later.
5. **Inputs/session:** validation, token replay, CSRF/CORS interplay, login errors, rate limiting, sensitive PII exposure and secrets.
6. **Failure/security:** Redis unavailable, token revocation, timeout and auditing without leaking credentials.

## Test strategy
For each identified risk, specify a reproduction using authorized local/test environments: setup, endpoint, principal/token, expected HTTP status, actual status if executed. Never claim an exploit or test result you haven't observed.

## Output
Report findings ordered by severity: title, impact, file/endpoint, evidence (verified vs suspected), reproduction/test, minimum safe fix and regression test. Mark unverified assumptions clearly. Do not silently patch other agents' files or merge PRs.
