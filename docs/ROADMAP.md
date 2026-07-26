# Shopee Clone — Lộ trình dự án học tập Microservices

## Bối cảnh

Dự án học tập clone Shopee, thiết kế theo kiến trúc microservices, deploy trên Kubernetes.

- `backend/` — dự án Maven multi-module (parent `pom.xml`, packaging=pom, groupId `com.shopee`), **JDK 25**, **Spring Boot 4.1.0**, **Spring Cloud 2025.1.2**.
  - `eureka-server` — hoàn chỉnh (service discovery).
  - `config-server` — hoàn chỉnh (cấu hình tập trung qua `config-repo/application.yml`, đã expose sẵn `health,info,metrics,prometheus` qua Actuator cho mọi service).
  - `api-gateway` — hoàn chỉnh (bản hiện tại). `JwtAuthenticationFilter` xác thực JWT ở tầng gateway và forward header `X-User-Id` / `X-User-Roles` xuống các service phía sau; danh sách `PUBLIC_PATHS` đánh dấu các route không cần xác thực. **Mọi service mới phải tin tưởng các header này thay vì tự parse JWT** (xem `HeaderAuthenticationFilter` trong `user-service`). ⚠️ Sẽ mở rộng theo thiết kế RBAC mới — xem `docs/architecture/rbac.md`: Gateway đảm nhiệm luôn việc **phân quyền** (permission theo route), không chỉ xác thực.
  - `user-service` — khá hoàn chỉnh: đăng ký/đăng nhập, phát hành JWT (JJWT 0.12.6), Postgres + Flyway, Lombok, Actuator. Đây là mẫu tham chiếu cho cấu trúc của mọi service mới (định dạng `application.yml`, `Dockerfile`, cấu trúc package).
  - `product-service` — **dang dở**: đã có controller/entity/repository/service, nhưng còn thiếu `Dockerfile`, `application.yml`, và thư mục Flyway migration đang trống.
- `frontend/` — hoàn toàn trống, chưa scaffold gì.
- `k8s/` — chỉ có thư mục rỗng `k8s/base/`.
- Không có `docker-compose.yml`, `README`, hay `.gitignore` ở bất kỳ đâu.

Mục tiêu: định nghĩa kiến trúc tổng thể và lộ trình triển khai **từng chức năng một** — làm từng phần trong các phiên làm việc riêng biệt.

**Các quyết định đã chốt:**
- Message broker cho sự kiện bất đồng bộ (order → notification): **Kafka**.
- Ngôn ngữ frontend: **TypeScript**.
- Styling frontend: **Tailwind CSS**.
- Phạm vi tính năng: **Full + Stretch** — luồng mua hàng cốt lõi + đánh giá/rating + notification-service + admin panel + search-service (Elasticsearch).
- Phân quyền: **RBAC đầy đủ** (Role + Permission lưu trong DB, 1 user nhiều role, mở rộng bằng data không cần sửa code), **kiểm tra permission tập trung tại API Gateway** thay vì rải rác từng service. Chi tiết: [`docs/architecture/rbac.md`](architecture/rbac.md).

**Các lựa chọn mặc định cho những quyết định nhỏ hơn** (có thể đổi bất cứ lúc nào, chi phí thay đổi thấp):
- Tồn kho vẫn nằm trong entity `Product` của `product-service` (không tách inventory-service riêng).
- Local/dev và lần deploy k8s đầu tiên: dùng chung 1 instance Postgres, chứa nhiều database logic (`shopee_user`, `shopee_product`, `shopee_order`, `shopee_review`), mỗi service vẫn chỉ tự quản lý Flyway migration cho database của mình.
- `cart-service` dùng **Redis**, không dùng Postgres (dữ liệu tạm, theo user, không cần quan hệ).
- `config-repo` của `config-server` vẫn được bake sẵn vào Docker image (giữ nguyên như hiện tại).
- Kubernetes: dùng **Kustomize** (`k8s/base` + `k8s/overlays/local`), khớp với thư mục `k8s/base` đã có sẵn.
- Cluster local: dùng **kind** (chạy tốt trên Docker Desktop/Windows).
- Luồng order/payment: gọi đồng bộ (sync REST) từ order-service sang payment-service (chưa dùng saga/outbox — để dành làm hardening sau này nếu muốn).
- Review: bất kỳ user đã đăng nhập nào cũng review được (không bắt buộc "đã mua hàng").
- Observability: Actuator health/metrics (đã có sẵn miễn phí qua config-repo) + Prometheus/Grafana (tùy chọn, làm gần cuối); bỏ qua distributed tracing/log tập trung mặc định.
- Thêm `docker-compose.yml` ở root sớm (Postgres, Redis, Kafka) để có vòng lặp dev nhanh trước khi đụng vào k8s.

---

## Kiến trúc tổng thể

### Các service (ngoài 5 service đã có)

| Service | Nhiệm vụ | Lưu trữ | Giao tiếp với |
|---|---|---|---|
| `cart-service` | Giỏ hàng theo từng user | Redis | `product-service` (kiểm tra giá/tồn kho khi thêm vào giỏ) |
| `order-service` | Checkout, vòng đời đơn hàng, lịch sử trạng thái | Postgres (`shopee_order`) | `cart-service`, `product-service` (trừ tồn kho), `payment-service` (sync), phát sự kiện Kafka |
| `payment-service` | Giả lập xử lý thanh toán | Postgres hoặc in-memory (`shopee_payment`) | Được `order-service` gọi đồng bộ |
| `notification-service` | Thông báo khi có sự kiện đơn hàng/thanh toán (giả lập gửi email qua log) | không cần lưu trữ (stateless consumer) | Consume sự kiện Kafka từ `order-service` |
| `review-service` | Đánh giá & rating sản phẩm | Postgres (`shopee_review`) | `product-service` (kiểm tra sản phẩm tồn tại) |
| `search-service` | Tìm kiếm full-text/faceted sản phẩm | Elasticsearch | Đồng bộ dữ liệu từ `product-service` (reindex khi tạo/sửa, hoặc qua sự kiện Kafka `ProductChanged`) |

### Giao tiếp giữa các service

- REST đồng bộ (WebClient/Feign — Spring Cloud đã có sẵn) cho: gateway→services, cart→product, order→cart, order→product, order→payment, review→product.
- **Kafka** cho các sự kiện bất đồng bộ phát ra từ `order-service`: `OrderCreated`, `OrderPaid`, `OrderShipped`, được `notification-service` consume. `search-service` cũng có thể consume thêm sự kiện `ProductChanged` từ `product-service` để đồng bộ dữ liệu, thay vì gọi reindex đồng bộ.
- Kafka chạy ở **chế độ KRaft** (không cần ZooKeeper) — 1 StatefulSet trong k8s, 1 container trong docker-compose.

### Frontend (Vue 3 + TypeScript + Vite + Tailwind)

```
frontend/
  src/
    api/          instance axios + 1 module cho mỗi domain (authApi, productApi, cartApi, orderApi, reviewApi, searchApi)
    stores/        Pinia: auth, cart, product, order
    router/         vue-router, route meta.requiresAuth / meta.requiresAdmin
    views/          Login, Register, Home, ProductDetail, Cart, Checkout, OrderHistory, Search, Admin/*
    components/     ProductCard, StarRating, CartBadge, ...
    layouts/        MainLayout, AuthLayout, AdminLayout
    composables/     useAuth, useCart
    types/           interface TS ánh xạ theo DTO backend
```

- API client luôn trỏ vào **api-gateway**, không bao giờ gọi thẳng microservice (bắt buộc theo thiết kế JWT của gateway).
- Auth store lưu JWT (localStorage); interceptor response của axios xử lý lỗi 401 → logout/redirect.
- Route admin được chặn theo claim `ROLE_ADMIN`, tái sử dụng các endpoint `@PreAuthorize("hasRole('ADMIN')")` đã có ở backend.

### Kubernetes

- `k8s/base`: Deployment/Service/ConfigMap/Secret dùng chung cho mỗi thành phần.
- `k8s/overlays/local`: phù hợp với kind (NodePort/Ingress, `imagePullPolicy: IfNotPresent`, resource request nhỏ).
- Tầng dữ liệu: Postgres (1 instance, script init nhiều DB), Redis, Kafka (StatefulSet KRaft) — đều là Deployment/StatefulSet + PVC.
- Mỗi Deployment service có `livenessProbe`/`readinessProbe` trỏ vào `/actuator/health/liveness|readiness` (cần bật `management.endpoint.health.probes.enabled: true` một lần trong `config-repo`).
- Một Ingress (nginx-ingress) duy nhất: `/` → frontend, `/api` → api-gateway.
- Dùng chung một namespace `shopee`.

---

## Lộ trình theo thứ tự (mỗi mục = một buổi làm việc)

1. **Hoàn thiện `product-service`** — `application.yml` (theo mẫu user-service: port riêng, DB `shopee_product`, flyway, eureka, config import, actuator), Flyway `V1__create_categories_and_products_table.sql`, `Dockerfile` (theo mẫu user-service), test thử end-to-end.
2. **Local dev harness** — `docker-compose.yml` ở root (Postgres, Redis, Kafka chế độ KRaft), `.gitignore`, `README.md` ngắn gọn hướng dẫn chạy local. Giúp vòng lặp phản hồi nhanh cho mọi phase sau.
3. **`cart-service`** — module Maven mới, tích hợp Redis, model Cart/CartItem, add-to-cart gọi `product-service` để kiểm tra giá/tồn kho, `application.yml` + `Dockerfile`, thêm route gateway `/api/cart/**` (cần xác thực).
4. **Scaffold frontend** — `npm create vue@latest` (TS + router + Pinia), setup Tailwind, layout/router cơ bản, axios client trỏ vào gateway, trang Login/Register + auth store + route guard.
5. **Frontend: trang danh mục & chi tiết sản phẩm** — ánh xạ với `product-service` (đã hỗ trợ sẵn phân trang/lọc category ở backend).
6. **Frontend: giao diện giỏ hàng** — trang giỏ hàng, nút add-to-cart, badge trên header — ánh xạ với phase 3.
7. **`order-service`** — module mới, entity Order/OrderItem + Postgres/Flyway, endpoint checkout (lấy giỏ hàng, kiểm tra/trừ tồn kho qua product-service, tạo đơn), endpoint my-orders/order-detail, admin xem tất cả.
8. **`payment-service`** — module mới, endpoint giả lập cổng thanh toán, được gọi đồng bộ từ luồng checkout của order-service.
9. **Frontend: luồng checkout & thanh toán** — form địa chỉ, tóm tắt đơn hàng, đặt hàng, thành công/thất bại — ánh xạ với phase 7-8.
10. **Frontend: lịch sử & chi tiết đơn hàng** — ánh xạ với phase 7.
11. **Kết nối Kafka + `notification-service`** — order-service phát sự kiện `OrderCreated`/`OrderPaid`/`OrderShipped`; module notification-service mới consume và log ("gửi") thông báo.
12. **`review-service`** — module mới, entity Review, tạo/liệt kê review theo sản phẩm, tính rating trung bình, kiểm tra sản phẩm tồn tại qua product-service.
13. **Frontend: giao diện đánh giá & rating** trên trang chi tiết sản phẩm — ánh xạ với phase 12.
14. **Frontend: admin panel** — CRUD sản phẩm/danh mục, quản lý trạng thái đơn hàng, tái sử dụng endpoint `ROLE_ADMIN` (có thể cần thêm endpoint admin update trạng thái đơn ở order-service).
15. **`search-service` + Elasticsearch** — module mới, index sản phẩm (load ban đầu + consumer sự kiện Kafka `ProductChanged` hoặc gọi reindex đồng bộ từ product-service), endpoint search/filter.
16. **Frontend: giao diện tìm kiếm** — thanh search/trang kết quả — ánh xạ với phase 15.
17. **k8s manifests, phần 1** — tầng dữ liệu (Postgres multi-DB, Redis, Kafka KRaft) + Secret/ConfigMap + manifest cho eureka-server/config-server.
18. **k8s manifests, phần 2** — Deployment+Service cho từng microservice backend, kèm health probe.
19. **k8s manifests, phần 3** — Dockerfile frontend (build→nginx) + Deployment/Service + Ingress trỏ vào frontend và api-gateway.
20. **Chạy end-to-end trên cluster kind local** — build/load toàn bộ image, `kubectl apply -k`, kiểm tra full luồng đăng ký→duyệt sản phẩm→tìm kiếm→giỏ hàng→checkout→thanh toán→lịch sử đơn→review qua Ingress.
21. **Observability** — bật health probe endpoint toàn bộ project, cài Helm chart `kube-prometheus-stack`, vài dashboard Grafana.
22. **Backlog / hardening tùy chọn** (không theo thứ tự, làm nếu còn hứng): refresh token, rate limiting ở gateway, upload ảnh (MinIO/S3), saga/outbox cho payment, CI pipeline, HPA, TLS, distributed tracing.

---

## Cách kiểm thử (áp dụng xuyên suốt)

- Mỗi phase backend: chạy `mvn spring-boot:run` (hoặc qua docker-compose) service liên quan, gọi thử endpoint bằng curl/Postman qua gateway, kiểm tra service đã đăng ký Eureka và Flyway migration chạy sạch.
- Mỗi phase frontend: `npm run dev`, thao tác thử trang/luồng mới trên trình duyệt với backend đang chạy local.
- Mỗi phase k8s: `kubectl apply -k k8s/overlays/local`, kiểm tra `kubectl get pods -n shopee` đều Running/Ready, xem `kubectl logs` để rà soát khởi động, rồi thử luồng qua Ingress.
- Phase 20 là bước kiểm tra end-to-end thật sự đầu tiên: toàn bộ hành trình người dùng qua Ingress thật trên cluster kind local.

## Các file hiện có cần dùng làm mẫu

- `backend/user-service/src/main/resources/application.yml` — mẫu cấu hình cho mọi service mới.
- `backend/user-service/Dockerfile` — mẫu Dockerfile cho mọi service mới.
- `backend/api-gateway/src/main/java/com/shopee/gateway/filter/JwtAuthenticationFilter.java` — quy ước forward header mà service mới phải tuân theo, và nơi thêm route/public path mới.
- `backend/config-server/src/main/resources/config-repo/application.yml` — config dùng chung; thêm cấu hình probe endpoint ở đây một lần, áp dụng cho mọi service.
- `backend/product-service/src/main/java/com/shopee/product/controller/ProductController.java` — mẫu cho `Pageable` listing + endpoint admin dùng `@PreAuthorize("hasRole('ADMIN')")`, tái sử dụng cho mọi endpoint admin sau này.
