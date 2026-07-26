# Môi trường dev local

> **Cập nhật:** Docker Desktop **đã cài và chạy được** (daemon lên bình thường, `docker version`/`docker run` hoạt động) — phần mô tả "không cài được Docker" bên dưới là lịch sử, giữ lại vì Postgres/Node/Maven portable vẫn đang được dùng song song (không có lý do phải di dời sang container). Từ giờ, thành phần hạ tầng mới (VD Redis cho refresh token) sẽ chạy bằng Docker container thay vì tìm bản portable.

Máy dev hiện tại **không cài Node.js/Maven** mặc định, và trước đây **Docker Desktop cài rồi nhưng daemon không chạy được** (thiếu WSL2 kernel, `wsl --update` lỗi "service cannot be started" — cần quyền admin để bật tính năng Windows). Vì vậy Node/Maven/Postgres được cài dạng **portable (giải nén, không cần admin)** vào `C:\Users\nc\tools`, thay cho `docker-compose` ở `docs/ROADMAP.md` phase 2. Phần này vẫn còn đúng và vẫn là cách chạy 3 tool đó.

## Đã cài sẵn tại `C:\Users\nc\tools`

| Tool | Đường dẫn | Ghi chú |
|---|---|---|
| Node.js 24.18.0 | `C:\Users\nc\tools\node` | Đã thêm vào **user PATH** (registry), nhưng mỗi phiên PowerShell mới cần set lại `$env:Path` nếu chạy qua tool tự động — xem lệnh mẫu bên dưới |
| Apache Maven 3.9.16 | `C:\Users\nc\tools\apache-maven-3.9.16` | Cần `JAVA_HOME` trỏ đúng `C:\Program Files\Java\jdk-25.0.3` (không phải `...\javapath`, đó chỉ là shim) |
| PostgreSQL 16.14 (portable, không installer) | `C:\Users\nc\tools\pgsql` (binaries), `C:\Users\nc\tools\pgsql-data` (data dir) | Chỉ có `postgres.exe`/`initdb.exe`/`pg_ctl.exe` — **không có `psql`/`createdb`**, dùng JDBC (xem bên dưới) nếu cần chạy SQL tay |
| Redis 7 (Docker container) | image `redis:7-alpine`, container tên `shopee-redis`, port `6379` | Dùng cho refresh token (`user-service`) — xem mục Redis bên dưới. Không cần volume/persistence riêng (mất refresh token khi container bị xoá chỉ bắt người dùng đăng nhập lại, không mất dữ liệu nghiệp vụ) |

## Lệnh khởi động nhanh (mỗi phiên PowerShell mới)

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.3"
$env:Path += ";$env:JAVA_HOME\bin;C:\Users\nc\tools\apache-maven-3.9.16\bin;C:\Users\nc\tools\node"
```

### Postgres

```powershell
$pgBin = "C:\Users\nc\tools\pgsql\bin"
$dataDir = "C:\Users\nc\tools\pgsql-data"
& "$pgBin\pg_ctl.exe" -D $dataDir -l "$dataDir\server.log" start   # start
& "$pgBin\pg_ctl.exe" -D $dataDir stop                              # stop
```

Cluster được khởi tạo với `--locale-provider=icu --icu-locale=vi-VN` (không phải `--locale=C` mặc định) — **bắt buộc** để `ILIKE`/case-insensitive search hoạt động đúng với ký tự có dấu tiếng Việt (VD: tìm "đèn" phải khớp được "Đèn bàn LED"). Nếu khởi tạo lại cluster từ đầu, nhớ giữ nguyên 2 flag này, nếu không search sẽ chỉ khớp đúng chữ hoa/thường y hệt byte-for-byte.

- Superuser: `shopee` / mật khẩu `shopee` (khớp default trong mọi `application.yml`), lắng nghe port mặc định `5432`.
- Database `shopee_user` đã được tạo. Tạo thêm DB khác (VD `shopee_product`) bằng cách chạy 1 câu JDBC (không có `createdb.exe`):
  ```powershell
  # cần postgresql-jdbc.jar (đã tải sẵn tại C:\Users\nc\tools\postgresql-jdbc.jar) và JAVA_HOME đã set
  # biên dịch/chạy 1 class Java nhỏ gọi Statement.execute("CREATE DATABASE ...") tới jdbc:postgresql://localhost:5432/postgres
  ```

### Redis (refresh token)

```powershell
docker run -d --name shopee-redis -p 6379:6379 --restart unless-stopped redis:7-alpine   # lần đầu
docker start shopee-redis                                                                # các lần sau
```

`user-service` cần Redis chạy trước khi start (Spring Boot fail-fast nếu không kết nối được lúc gọi `/register`/`/login`/`/refresh`/`/logout` — các endpoint khác không bị ảnh hưởng). Xem cấu hình ở `jwt.refresh-expiration-days` (mặc định 30 ngày, override bằng `JWT_REFRESH_EXPIRATION_DAYS`) và `app.cookie.secure` (mặc định `false` cho dev http, override bằng `COOKIE_SECURE=true` khi chạy sau HTTPS thật).

### Backend (Maven, chạy tuần tự: eureka → config-server → api-gateway → user-service → product-service)

```powershell
cd backend
mvn -q -DskipTests install          # build toàn bộ reactor

cd eureka-server;    java -jar target\eureka-server.jar   # port 8761
cd config-server;    java -jar target\config-server.jar   # port 8888
cd api-gateway;      java -jar target\api-gateway.jar     # port 8080
cd user-service;     java -jar target\user-service.jar    # port 8081
cd product-service;  java -jar target\product-service.jar # port 8082
```

Nên khởi động **tuần tự và chờ health** (`GET http://localhost:<port>/actuator/health` trả 200) trước khi bật service kế tiếp, tránh lỗi do chưa đăng ký Eureka/config-server kịp.

⚠️ **Sau khi tất cả service đã "healthy", đợi thêm ~20-30 giây trước khi gọi qua Gateway** — `api-gateway` dùng Spring Cloud LoadBalancer, cache danh sách instance từ Eureka có độ trễ riêng (khác với việc service đã đăng ký xong ở Eureka). Gọi qua gateway ngay sau khi mọi actuator/health đều 200 sẽ ra `503 Service Unavailable` dù mọi service đều chạy tốt — không phải bug, chỉ cần đợi thêm.

⚠️ **Sau khi sửa `pom.xml`/cấu hình plugin (không phải sửa file `.java`), phải chạy `mvn clean install`, không chỉ `mvn install`** — Maven's incremental compiler chỉ so sánh timestamp source `.java`, không phát hiện thay đổi cấu hình compiler (VD: cờ `-parameters`), nên có thể âm thầm dùng lại class đã compile từ trước với cấu hình cũ.

### Frontend

```powershell
cd frontend
npm install
npm run dev   # http://localhost:5173
```

## Các lỗi đã gặp & đã sửa trong code (không phải do môi trường)

Khi build/chạy thật lần đầu, phát hiện và sửa các lỗi sau (đã áp dụng vào code, không phải workaround tạm thời):

1. **Lombok không sinh getter/setter/builder** — `backend/pom.xml` (parent) thiếu `annotationProcessorPaths` cho Lombok trong `pluginManagement` của `maven-compiler-plugin`. Đã thêm.
2. **Jar chạy ra chỉ vài KB (không phải fat jar)** — do dự án không kế thừa `spring-boot-starter-parent` nên thiếu `<executions><goal>repackage</goal></executions>` cho `spring-boot-maven-plugin`. Đã thêm vào `backend/pom.xml` pluginManagement.
3. **`api-gateway` báo lỗi "No spring.config.import property has been defined"** — thiếu `spring.config.import: optional:configserver:...` trong `api-gateway/application.yml` (user-service đã có sẵn dòng này, gateway thì thiếu). Đã thêm.
4. **Flyway không chạy, Hibernate báo "missing table [users]"** — Spring Boot 4.1.0 tách autoconfiguration của Flyway ra module riêng `org.springframework.boot:spring-boot-flyway` (không còn tự động kèm theo `flyway-core`). Đã thêm dependency này vào `user-service/pom.xml`.
5. **Lỗi nghiệp vụ (409 email trùng, 401 sai mật khẩu) trả về 403 rỗng thay vì đúng mã lỗi kèm message** — `server.error.include-message: always` không có tác dụng trên Spring Boot 4.1.0 (có thể do cấu trúc lại tương tự mục 4), và path `/error` không nằm trong `permitAll` của `SecurityConfig` nên bị chặn 403 khi Spring MVC forward nội bộ sang `/error`. Đã sửa 2 chỗ:
   - Thêm `"/error"` vào danh sách `permitAll` trong `SecurityConfig.java`.
   - Thêm `ApiExceptionHandler` (`@RestControllerAdvice`) trong `user-service` để tự trả JSON `{status, error, message}` cho `ResponseStatusException` và lỗi validate, không phụ thuộc cơ chế mặc định của Boot.
6. **Message lỗi bị thiếu dấu tiếng Việt** ("Email hoac mat khau khong dung" thay vì "Email hoặc mật khẩu không đúng") — sửa lại các chuỗi trong `AuthService.java`, `UserController.java`, `RegisterRequest.java` cho khớp với bản có dấu trong `docs/features/01-auth.md`.
7. **Thiếu cờ compiler `-parameters`** — `@PathVariable UUID id` / `@RequestParam UUID categoryId` (không đặt tên tường minh) làm `product-service` lỗi 500 `IllegalArgumentException: Name for argument ... not specified`. Do dự án không kế thừa `spring-boot-starter-parent` (vốn tự bật cờ này). Đã thêm `<parameters>true</parameters>` vào `maven-compiler-plugin` trong `backend/pom.xml` pluginManagement.
8. **Gateway coi `/api/products` là public cho MỌI method** (kể cả POST/PUT/DELETE), nên khi request có token hợp lệ đi qua route public, Gateway bỏ qua luôn bước giải mã JWT và **không forward `X-User-Id`/`X-User-Roles`** xuống service — làm mọi request ghi (kể cả của Admin) bị coi là ẩn danh. Đã sửa `JwtAuthenticationFilter` ở `api-gateway`: tách `PUBLIC_PATHS` (public mọi method — login/register/health) khỏi `PUBLIC_GET_PREFIXES` (chỉ public với GET — `/api/products`, `/api/categories`), method khác vẫn phải qua xác thực JWT như bình thường.
9. **`LazyInitializationException` khi map `Product.category` sang `categoryName`** — do `open-in-view: false` (chủ đích, tránh anti-pattern OSIV) khiến Hibernate session đóng trước khi `ProductResponse.from()` chạy trong `ProductService`. Lúc đó đã fix tạm bằng `@EntityGraph(attributePaths = "category")` — **nhưng sau đó đã bỏ hẳn `@ManyToOne`/`@EntityGraph` này** theo quy ước mới (mục 12), category giờ chỉ là `categoryId` phẳng + batch-fetch thủ công, không còn phụ thuộc lazy loading nữa nên bug này không còn khả năng tái diễn.
10. **Tìm kiếm tiếng Việt không phân biệt hoa/thường bị sai** (VD: tìm "đèn" không ra "Đèn bàn LED") — do cluster Postgres portable ban đầu khởi tạo với `--locale=C`, không case-fold đúng ký tự ngoài ASCII. Đã khởi tạo lại cluster với `--locale-provider=icu --icu-locale=vi-VN` (xem mục Postgres ở trên). Đây là vấn đề hạ tầng cục bộ, không phải lỗi code — Postgres deploy thật (VD image Docker mặc định) thường đã dùng locale UTF-8 chuẩn nên không gặp vấn đề này.

Đã verify end-to-end qua Playwright cho cả 2 chức năng Auth và Product Catalog (đăng ký → tự đăng nhập → xem `/me` → đăng xuất → đăng nhập đúng/sai → đăng ký trùng email; danh sách sản phẩm → lọc theo danh mục → tìm kiếm có dấu → trạng thái rỗng → chi tiết sản phẩm → 404), tất cả hiển thị đúng thông báo tiếng Việt như wireframe.

11. **RBAC implement xong** (`docs/architecture/rbac.md`) — `user-service` có Flyway V2 (tạo bảng `roles`/`permissions`/`user_roles`/`role_permissions`, seed data, **migrate tự động dữ liệu `role` cũ sang bảng mới** bằng 1 câu `INSERT ... SELECT`, đã verify 2 user thật tạo từ trước migration vẫn giữ đúng role) và V3 (drop cột `role`). `api-gateway` đọc `config-repo/api-gateway.yml` (3 nhóm `public-routes`/`authenticated-only`/`permission-required`) qua `AuthorizationProperties`, tự chặn 403 theo permission trước khi tới service. `product-service` bỏ hẳn `@PreAuthorize`. **Frontend đổi breaking**: `role: string` → `roles: string[]` trong `types/auth.ts`/`stores/auth.ts`/`AccountView.vue` — nếu quên đổi FE khi backend đã đổi, `/me` và trang tài khoản sẽ hiển thị sai/lỗi im lặng (không throw, chỉ hiện `undefined`).
12. **Bỏ hẳn JPA relationship (`@ManyToMany`/`@ManyToOne`/`@OneToMany`) trong toàn bộ entity** — quyết định kiến trúc theo yêu cầu (xem `docs/architecture/rbac.md` §2 "Quy ước quan trọng"), tránh object graph của Hibernate khó kiểm soát khi hệ thống lớn dần. `User`/`Role` không còn field quan hệ; join `user_roles`/`role_permissions` bằng **native SQL query** trong `RoleRepository`. `Product` không còn `@ManyToOne Category` mà chỉ có `categoryId` (UUID phẳng); lấy `categoryName` bằng cách batch-fetch `categoryRepository.findAllById(...)` một lần rồi tự ghép trong `ProductService`. Lỗi gặp phải khi refactor: `AuthService.register()` gọi `@Modifying` native query (insert vào `user_roles`) nhưng thiếu `@Transactional` → `TransactionRequiredException`/500 — native update/insert **luôn cần `@Transactional`** dù chỉ có 1 câu lệnh, khác với native SELECT.
13. **Đã cài xong Docker Desktop** — daemon chạy bình thường (`docker version` OK). Trong lúc cài đặt, máy đã **restart**, làm chết toàn bộ process `java` (5 service) lẫn `postgres.exe` đang chạy nền, và Postgres bị tắt không sạch (unclean shutdown). Ghi nhận lại vì sẽ gặp lại mỗi lần máy restart:
    - `pg_ctl start` tự động chạy WAL redo recovery khi phát hiện shutdown không sạch — thấy log `database system was interrupted`/`automatic recovery in progress` là **bình thường**, không phải lỗi, cứ đợi tới dòng `database system is ready to accept connections`.
    - Phải khởi động lại **toàn bộ** theo đúng thứ tự (Postgres → eureka → config-server → api-gateway → user-service/product-service → `npm run dev`) — không có gì tự khởi động lại cùng Windows.
14. **Thêm refresh token cho Auth, dùng Redis (Docker) làm nơi lưu** — xem `docs/features/01-auth.md` mục 4.5-4.6 cho luồng đầy đủ. Điểm kỹ thuật đáng chú ý:
    - Refresh token là chuỗi ngẫu nhiên (opaque), **không phải JWT** — lý do là JWT không tự thu hồi được trước hạn nếu không lưu thêm state, nên dùng opaque token + Redis để `/logout` xoá được ngay.
    - Chỉ gửi qua `Set-Cookie` **httpOnly**, không bao giờ vào JSON body/localStorage — bắt buộc frontend (`http.ts`) phải bật `withCredentials: true` thì trình duyệt mới lưu/gửi cookie này (thiếu cờ này là nguyên nhân phổ biến nhất khi debug "sao không thấy cookie đâu").
    - Viết test PowerShell/Playwright cho cookie **httpOnly có `Path` hẹp** (ở đây là `/api/users/auth`) dễ dính bẫy: `$session.Cookies.GetCookies("http://host:port")` (.NET) và `context.cookies("http://host:port")` (Playwright) đều lọc cookie theo path-matching chuẩn RFC 6265 — gọi với path gốc `/` sẽ **không** trả về cookie có `Path` cụ thể hơn, trông như "cookie không được set" dù server đã set đúng. Phải gọi `context.cookies()` không tham số (lấy hết) hoặc truyền đúng URL có path khớp.
    - Dùng `New-Object Microsoft.PowerShell.Commands.WebRequestSession` thủ công thì `.Cookies` là `null` cho tới khi có response đầu tiên populate qua tham số `-WebSession`; phải dùng `-SessionVariable session` ngay ở request đầu tiên để PowerShell tự khởi tạo và điền cookie jar, nếu không các request sau sẽ không gửi kèm cookie nào cả (trông giống lỗi 401 phía server nhưng thực ra là lỗi ở test script).

## Docker

Docker Desktop đã cài và chạy được trên máy này. Redis cho refresh token (mục Redis ở trên) đang chạy qua Docker container độc lập (`docker run`), chưa gộp vào `docker-compose.yml`. Việc dựng `docker-compose.yml` đầy đủ (Postgres, Redis, Kafka) theo `docs/ROADMAP.md` phase 2 và di dời Postgres/Kafka còn lại sang Docker vẫn là việc **chưa làm**, để dành khi bắt đầu `cart-service` (cần Kafka) hoặc khi muốn thống nhất toàn bộ hạ tầng local về container thay vì portable binaries.
