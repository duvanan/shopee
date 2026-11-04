# Shopee Clone — Feature Inventory & Backlog

> Căn cứ: code trên `main` ngày 04/11/2026. ✅ dưới đây nghĩa là **đã tìm thấy implementation**, **không có nghĩa là đã nghiệm thu runtime**. Các tính năng có thể thay đổi sau khi audit S1.

## A. Đã tìm thấy code — cần baseline verification (S1)

| Module | Feature | Bằng chứng |
| --- | --- | --- |
| Authentication | Register, login, refresh, refresh rotation, logout | `user-service/AuthController`, `AuthService`, `RefreshTokenService` |
| User | Get current account (/me) | `UserController` |
| Security | BCrypt, JWT claims, RBAC roles/permissions in Postgres, route permission enforcement | `JwtService`, `RoleRepository`, `api-gateway/JwtAuthenticationFilter` |
| Product | List, detail, search name, category filter, pagination, CRUD | `ProductController`, `ProductService` |
| Category | List, create, update, delete, reject delete if associated products | `CategoryController` |
| Frontend | Login/register/account, home catalog/detail, admin products/categories, route guards, silent refresh | `frontend/src/views`, `router`, `stores/auth.ts`, `api/http.ts` |
| Platform | Eureka, Config Server, Gateway, Flyway, Actuator, traceId/MDC, shared API error schema | `backend/` |

## B. Scheduled Features (Issues [S1]...[S8])

| Group | Deliverable | Priority | Sprint |
| --- | --- | --- | --- |
| Baseline | Tests, security review, CI, local Compose | P0 | S1 |
| Cart | Redis cart, user isolation, cart UI | P0 | S2 |
| Order | Create/detail/history/order state | P0 | S3 |
| Inventory | Atomic stock update / consistency tests | P0 | S3 |
| Payment | Mock payment, idempotency, checkout integration | P0 | S4 |
| MVP Quality | Full E2E, failure/negative cases, docs | P0 | S5 |
| Admin Security | Role/permission/user management, token change policy | P1 | S6 |
| Review | Review CRUD/UI | P1 | S7 |
| Events | Kafka order events + notification consumer | P1 | S7 |
| Operations | Kafka KRaft 3-node, ELK pipeline, failure test | P1 | S8 |

## C. Unscheduled Backlog — không cam kết deadline
1. Seller onboarding / multi-vendor ownership.
2. Storefront người bán, product ownership.
3. Vouchers/discount codes.
4. Flash sale.
5. Elasticsearch full-text/faceted search (khác tìm tên hiện tại).
6. Product image upload (object storage).
7. Product variants/SKUs.
8. Multi-address book và phí vận chuyển thực tế.
9. Vận chuyển/tích hợp logistics.
10. Refund/return flows.
11. Wishlist/favorites.
12. Customer chat.
13. Advanced analytics/admin reporting.
14. Kubernetes/Kustomize deployment.
15. Distributed tracing OpenTelemetry/Jaeger.
16. CI/CD production deployments, SLO/alerts.

**Chưa có code** = không phát hiện module/API tương ứng trong cây repo, không phải tuyên bố tính năng tuyệt đối không tồn tại ngoài GitHub.

## Cách thêm feature
1. Viết User story và AC trong Issue template.
2. Xếp P0/P1/P2, estimate theo giờ; phân dependency.
3. Chỉ commit vào sprint khi capacity còn đủ.
4. Tạo branch/PR, test, nghiệm thu như `WORKFLOW.md`.
