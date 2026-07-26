# Chuẩn Response API cho Java Spring Boot

Tài liệu này định nghĩa chuẩn chung cho dữ liệu trả về từ backend Java/Spring Boot cho phía client. Mục tiêu là giúp toàn bộ API có cấu trúc thống nhất, dễ tích hợp với frontend, dễ xử lý lỗi, dễ giám sát và thuận tiện khi mở rộng hệ thống.

> **Trạng thái trong dự án này:** đã áp dụng cho `user-service` và `product-service` — xem mục 21 "Áp dụng trong shopee-clone" ở cuối tài liệu để biết khác biệt so với bản chuẩn gốc (package thực tế, cách chia sẻ code giữa các service không dùng module dùng chung).

---

## 1. Nguyên tắc chung

Toàn bộ API nên thống nhất theo ba nhóm response:

1. Response thành công.
2. Response danh sách có phân trang.
3. Response lỗi.

Không nên để mỗi API trả về một cấu trúc khác nhau.

Các trường dữ liệu chính:

| Trường | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| `success` | `boolean` | Trạng thái xử lý thành công hoặc thất bại |
| `code` | `string` | Mã kết quả hoặc mã lỗi cố định |
| `message` | `string` | Nội dung hiển thị cho người dùng |
| `data` | `object` | Dữ liệu nghiệp vụ |
| `errors` | `array` | Danh sách lỗi chi tiết |
| `meta` | `object` | Metadata của request/response |
| `timestamp` | `datetime` | Thời gian trả response |
| `traceId` | `string` | Mã truy vết request |
| `path` | `string` | API xảy ra lỗi |

---

## 2. Response thành công

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": {
    "id": 1,
    "name": "Nguyễn Văn A"
  },
  "meta": {
    "timestamp": "2026-07-26T12:30:45.123Z",
    "traceId": "9bd86cb74a164983"
  }
}
```

---

## 3. Response danh sách có phân trang

Quy ước:

- Client sử dụng `page` bắt đầu từ `1`.
- Spring Data sử dụng page bắt đầu từ `0`.
- Backend phải tự chuyển đổi giữa hai giá trị này.
- Nên giới hạn số bản ghi tối đa mỗi trang.

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": {
    "items": [
      {
        "id": 1,
        "name": "Nguyễn Văn A"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false
  },
  "meta": {
    "timestamp": "2026-07-26T12:30:45.123Z",
    "traceId": "9bd86cb74a164983"
  }
}
```

---

## 4. Response lỗi validation

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": [
    {
      "field": "username",
      "message": "Tên đăng nhập không được để trống"
    }
  ],
  "meta": {
    "timestamp": "2026-07-26T12:30:45.123Z",
    "traceId": "9bd86cb74a164983",
    "path": "/api/v1/users"
  }
}
```

---

## 5. Cấu trúc package đề xuất

```text
vn.viettel.starlink
└── common
    ├── exception
    │   ├── BusinessException.java
    │   ├── ErrorCode.java
    │   └── GlobalExceptionHandler.java
    └── response
        ├── ApiMeta.java
        ├── ApiResponse.java
        ├── FieldErrorDetail.java
        └── PageData.java
```

---

## 6. ApiMeta.java

```java
package vn.viettel.starlink.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiMeta(
        Instant timestamp,
        String traceId,
        String path
) {

    public static ApiMeta success(String traceId) {
        return new ApiMeta(
                Instant.now(),
                traceId,
                null
        );
    }

    public static ApiMeta error(String traceId, String path) {
        return new ApiMeta(
                Instant.now(),
                traceId,
                path
        );
    }
}
```

---

## 7. FieldErrorDetail.java

```java
package vn.viettel.starlink.common.response;

public record FieldErrorDetail(
        String field,
        String message
) {
}
```

---

## 8. ApiResponse.java

```java
package vn.viettel.starlink.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        List<FieldErrorDetail> errors,
        ApiMeta meta
) {

    private static final String TRACE_ID = "traceId";

    public static <T> ApiResponse<T> success(T data) {
        return success("SUCCESS", "Thành công", data);
    }

    public static <T> ApiResponse<T> success(
            String code,
            String message,
            T data
    ) {
        return new ApiResponse<>(
                true,
                code,
                message,
                data,
                null,
                ApiMeta.success(getTraceId())
        );
    }

    public static ApiResponse<Void> success(
            String code,
            String message
    ) {
        return new ApiResponse<>(
                true,
                code,
                message,
                null,
                null,
                ApiMeta.success(getTraceId())
        );
    }

    public static ApiResponse<Void> error(
            String code,
            String message,
            String path
    ) {
        return error(code, message, null, path);
    }

    public static ApiResponse<Void> error(
            String code,
            String message,
            List<FieldErrorDetail> errors,
            String path
    ) {
        return new ApiResponse<>(
                false,
                code,
                message,
                null,
                errors,
                ApiMeta.error(getTraceId(), path)
        );
    }

    private static String getTraceId() {
        return MDC.get(TRACE_ID);
    }
}
```

---

## 9. PageData.java

```java
package vn.viettel.starlink.common.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageData<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageData<T> from(Page<T> pageData) {
        return new PageData<>(
                pageData.getContent(),
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages(),
                pageData.isFirst(),
                pageData.isLast()
        );
    }

    public static <E, T> PageData<T> from(
            Page<E> pageData,
            Function<E, T> mapper
    ) {
        List<T> items = pageData.getContent()
                .stream()
                .map(mapper)
                .toList();

        return new PageData<>(
                items,
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages(),
                pageData.isFirst(),
                pageData.isLast()
        );
    }
}
```

---

## 10. ErrorCode.java

Toàn bộ mã lỗi phải được quản lý tập trung.

Không nên viết trực tiếp chuỗi mã lỗi ở nhiều vị trí trong source code.

```java
package vn.viettel.starlink.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_ERROR(
            "VALIDATION_ERROR",
            "Dữ liệu đầu vào không hợp lệ",
            HttpStatus.BAD_REQUEST
    ),

    INVALID_REQUEST(
            "INVALID_REQUEST",
            "Yêu cầu không hợp lệ",
            HttpStatus.BAD_REQUEST
    ),

    UNAUTHORIZED(
            "UNAUTHORIZED",
            "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn",
            HttpStatus.UNAUTHORIZED
    ),

    ACCESS_DENIED(
            "ACCESS_DENIED",
            "Bạn không có quyền thực hiện chức năng này",
            HttpStatus.FORBIDDEN
    ),

    RESOURCE_NOT_FOUND(
            "RESOURCE_NOT_FOUND",
            "Không tìm thấy dữ liệu",
            HttpStatus.NOT_FOUND
    ),

    DATA_ALREADY_EXISTS(
            "DATA_ALREADY_EXISTS",
            "Dữ liệu đã tồn tại",
            HttpStatus.CONFLICT
    ),

    BUSINESS_ERROR(
            "BUSINESS_ERROR",
            "Không thể thực hiện yêu cầu",
            HttpStatus.UNPROCESSABLE_ENTITY
    ),

    INTERNAL_SERVER_ERROR(
            "INTERNAL_SERVER_ERROR",
            "Hệ thống đang xảy ra lỗi, vui lòng thử lại sau",
            HttpStatus.INTERNAL_SERVER_ERROR
    );

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
```

---

## 11. BusinessException.java

```java
package vn.viettel.starlink.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String customMessage;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    public BusinessException(
            ErrorCode errorCode,
            String customMessage
    ) {
        super(customMessage);
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }

    public String getResponseMessage() {
        return customMessage != null && !customMessage.isBlank()
                ? customMessage
                : errorCode.getMessage();
    }
}
```

Ví dụ sử dụng:

```java
throw new BusinessException(
        ErrorCode.RESOURCE_NOT_FOUND,
        "Không tìm thấy người dùng có ID: " + id
);
```

---

## 12. GlobalExceptionHandler.java

```java
package vn.viettel.starlink.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.viettel.starlink.common.response.ApiResponse;
import vn.viettel.starlink.common.response.FieldErrorDetail;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        ApiResponse<Void> response = ApiResponse.error(
                errorCode.getCode(),
                exception.getResponseMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorDetail> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorDetail)
                .toList();

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;

        ApiResponse<Void> response = ApiResponse.error(
                errorCode.getCode(),
                errorCode.getMessage(),
                errors,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorDetail> errors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new FieldErrorDetail(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;

        ApiResponse<Void> response = ApiResponse.error(
                errorCode.getCode(),
                errorCode.getMessage(),
                errors,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected error at path: {}",
                request.getRequestURI(),
                exception
        );

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        ApiResponse<Void> response = ApiResponse.error(
                errorCode.getCode(),
                errorCode.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    private FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
        return new FieldErrorDetail(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }
}
```

Lưu ý:

- Không trả trực tiếp `exception.getMessage()` cho client khi xảy ra lỗi hệ thống.
- Không trả stack trace.
- Không trả câu SQL.
- Không trả tên bảng, đường dẫn file hoặc thông tin hạ tầng nội bộ.
- Chi tiết lỗi phải được ghi vào log kèm `traceId`.

---

## 13. Sử dụng trong Controller

```java
package vn.viettel.starlink.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.starlink.common.response.ApiResponse;
import vn.viettel.starlink.common.response.PageData;
import vn.viettel.starlink.user.dto.UserCreateRequest;
import vn.viettel.starlink.user.dto.UserResponse;
import vn.viettel.starlink.user.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody UserCreateRequest request
    ) {
        UserResponse response = userService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "USER_CREATED",
                        "Tạo người dùng thành công",
                        response
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.findById(id))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<UserResponse>>> search(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageData<UserResponse> response = userService.search(page, size);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 14. Xử lý phân trang trong Service

```java
int pageIndex = Math.max(page - 1, 0);
int pageSize = Math.min(Math.max(size, 1), 100);

Pageable pageable = PageRequest.of(
        pageIndex,
        pageSize,
        Sort.by(Sort.Direction.DESC, "createdAt")
);
```

Quy tắc:

- `page <= 0` được chuyển thành trang đầu tiên.
- `size <= 0` được chuyển thành `1`.
- `size > 100` được giới hạn còn `100`.
- Mặc định nên sử dụng `page = 1`, `size = 20`.

---

## 15. Quy ước HTTP Status

| Trường hợp | HTTP Status |
|---|---:|
| Lấy dữ liệu thành công | `200 OK` |
| Tạo mới thành công | `201 Created` |
| Cập nhật thành công | `200 OK` |
| Xóa thành công, không trả body | `204 No Content` |
| Sai dữ liệu đầu vào | `400 Bad Request` |
| Chưa đăng nhập | `401 Unauthorized` |
| Không có quyền | `403 Forbidden` |
| Không tìm thấy dữ liệu | `404 Not Found` |
| Dữ liệu trùng | `409 Conflict` |
| Lỗi nghiệp vụ | `422 Unprocessable Entity` |
| Lỗi hệ thống | `500 Internal Server Error` |

Không nên để toàn bộ API luôn trả HTTP `200` rồi đặt lỗi trong body.

Việc trả đúng HTTP status giúp:

- Frontend xử lý chính xác.
- API Gateway nhận biết request lỗi.
- Monitoring thống kê chính xác.
- Log và cảnh báo dễ kiểm soát.
- Công cụ kiểm thử API hoạt động đúng chuẩn.

---

## 16. Quy ước mã lỗi

Mã lỗi phải:

- Viết hoa.
- Sử dụng dấu gạch dưới.
- Không chứa dấu tiếng Việt.
- Có ý nghĩa rõ ràng.
- Không phụ thuộc vào nội dung `message`.
- Không thay đổi tùy theo ngôn ngữ hiển thị.

Ví dụ:

```text
SUCCESS
USER_CREATED
VALIDATION_ERROR
INVALID_REQUEST
RESOURCE_NOT_FOUND
USER_NOT_FOUND
DATA_ALREADY_EXISTS
USERNAME_ALREADY_EXISTS
UNAUTHORIZED
ACCESS_DENIED
BUSINESS_ERROR
INTERNAL_SERVER_ERROR
```

Frontend nên xử lý nghiệp vụ dựa vào `code`, không nên xử lý dựa vào `message`.

---

## 17. Quy ước dữ liệu trả về

### 17.1. Không trả trực tiếp JPA Entity

Không nên:

```java
@GetMapping("/{id}")
public UserEntity findById(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();
}
```

Nên:

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponse>> findById(
        @PathVariable Long id
) {
    return ResponseEntity.ok(
            ApiResponse.success(userService.findById(id))
    );
}
```

Lý do:

- Tránh lộ cấu trúc database.
- Tránh vòng lặp khi serialize quan hệ JPA.
- Tránh trả thừa dữ liệu.
- Dễ thay đổi cấu trúc database mà không ảnh hưởng client.
- Dễ kiểm soát dữ liệu nhạy cảm.

### 17.2. Chuẩn thời gian

Thời gian trả về nên sử dụng chuẩn ISO-8601.

Ví dụ:

```json
{
  "createdAt": "2026-07-26T12:30:45.123Z"
}
```

Nên lưu thời gian hệ thống theo UTC.

Frontend tự chuyển đổi sang múi giờ của người dùng.

### 17.3. Giá trị rỗng

Nên dùng:

- Danh sách rỗng: `[]`
- Dữ liệu không tồn tại: `null`
- Chuỗi không có dữ liệu: cân nhắc `null` thay vì `""`

Ví dụ:

```json
{
  "items": [],
  "totalElements": 0
}
```

Không nên trả:

```json
{
  "items": null,
  "totalElements": 0
}
```

---

## 18. Trace ID

Mỗi request nên có một `traceId`.

`traceId` được dùng để:

- Tra log giữa các microservice.
- Tra request tại API Gateway.
- Tra lỗi từ phía client.
- Liên kết log giữa Kafka producer và consumer.
- Điều tra lỗi trên môi trường production.

Ví dụ log:

```text
2026-07-26 19:30:45 INFO traceId=9bd86cb74a164983 Request GET /api/v1/users/1
```

Ví dụ response:

```json
{
  "success": false,
  "code": "INTERNAL_SERVER_ERROR",
  "message": "Hệ thống đang xảy ra lỗi, vui lòng thử lại sau",
  "meta": {
    "timestamp": "2026-07-26T12:30:45.123Z",
    "traceId": "9bd86cb74a164983",
    "path": "/api/v1/users/1"
  }
}
```

---

## 19. Quy tắc áp dụng bắt buộc

1. Tất cả API trả body phải sử dụng `ApiResponse<T>`.
2. API phân trang phải sử dụng `PageData<T>`.
3. Client truyền page bắt đầu từ `1`.
4. Backend giới hạn tối đa `100` bản ghi mỗi trang.
5. Mã lỗi được quản lý tập trung trong `ErrorCode`.
6. Lỗi nghiệp vụ phải throw `BusinessException`.
7. Lỗi hệ thống phải được xử lý trong `GlobalExceptionHandler`.
8. Không trả trực tiếp JPA Entity.
9. Không trả stack trace hoặc exception message nội bộ.
10. Không trả câu SQL, tên bảng hoặc đường dẫn file nội bộ.
11. Frontend xử lý logic bằng `code`.
12. `message` chỉ dùng để hiển thị.
13. Response lỗi phải có `traceId`.
14. Thời gian sử dụng chuẩn ISO-8601.
15. HTTP status phải phản ánh đúng kết quả xử lý.

---

## 20. Kết luận

Chuẩn response đề xuất:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": {},
  "errors": [],
  "meta": {
    "timestamp": "2026-07-26T12:30:45.123Z",
    "traceId": "9bd86cb74a164983",
    "path": null
  }
}
```

Trong đó:

- `code` dùng cho frontend xử lý logic.
- `message` dùng để hiển thị.
- `data` chứa dữ liệu nghiệp vụ.
- `errors` chứa lỗi chi tiết.
- `meta` chứa thông tin kỹ thuật hỗ trợ truy vết.
- HTTP status phản ánh chính xác kết quả request.

Chuẩn này nên được dùng làm quy ước bắt buộc cho toàn bộ Java Spring Boot code base.

---

## 21. Áp dụng trong shopee-clone

Phần này ghi lại khác biệt thực tế so với bản chuẩn gốc ở trên (mục 1-20), sau khi rà soát toàn bộ `backend/`.

### 21.1. Package và module dùng chung

- Package thực tế: `com.shopee.common` (thay vì `vn.viettel.starlink.common`).
- `ApiResponse`, `ApiMeta`, `PageData`, `FieldErrorDetail`, `ErrorCode`, `BusinessException`, `GlobalExceptionHandler` nằm trong module Maven `backend/common`, được `user-service` và `product-service` khai báo như một dependency (`<artifactId>common</artifactId>`).
- Cả hai service dùng `@SpringBootApplication(scanBasePackages = "com.shopee")` nên `GlobalExceptionHandler` (đặt trong `common`) tự động được Spring nhận mà không cần khai báo lại ở từng service.
- `ErrorCode` được mở rộng thêm mã lỗi riêng cho từng service (`EMAIL_ALREADY_EXISTS`, `USER_NOT_FOUND`, `REFRESH_TOKEN_MISSING`, `REFRESH_TOKEN_INVALID`, `PRODUCT_NOT_FOUND`, `CATEGORY_NOT_FOUND`, `CATEGORY_HAS_PRODUCTS`...) gộp chung trong 1 enum của module `common`, thay vì mỗi service tự định nghĩa enum riêng.

### 21.2. `api-gateway` không dùng module `common`

`api-gateway` chạy trên Spring Cloud Gateway (WebFlux/Netty). Module `common` phụ thuộc `spring-webmvc` (cho `GlobalExceptionHandler`, dùng `HttpServletRequest`) — nếu `api-gateway` khai báo dependency vào `common`, `spring-webmvc` sẽ lọt vào classpath và xung đột với yêu cầu "không có Spring MVC trên classpath" của Spring Cloud Gateway.

Vì vậy `api-gateway` tự dựng response lỗi 401/403 của riêng nó (tại `JwtAuthenticationFilter.respondWith`), dùng `ObjectMapper` để build JSON đúng hình dạng `ApiResponse<Void>` (`success`, `code`, `message`, `meta.timestamp/traceId/path`), thay vì throw `BusinessException`/dùng `GlobalExceptionHandler`. Mã lỗi và message dùng lại đúng nội dung của `ErrorCode.UNAUTHORIZED` / `ErrorCode.ACCESS_DENIED` trong `common`, giữ đồng bộ nội dung dù không share được code.

Các response đi qua Gateway tới được `user-service`/`product-service` (không bị chặn ở Gateway) vẫn dùng đúng `ApiResponse` chuẩn từ module `common` như bình thường — chỉ 2 trường hợp Gateway tự chặn trước khi tới service (thiếu/sai token, thiếu quyền) là tự xây body riêng.

### 21.3. Lan truyền `traceId` qua header `X-Trace-Id`

Bản chuẩn gốc giả định `traceId` có sẵn trong MDC nhưng không định nghĩa ai là người tạo ra nó. Trong shopee-clone:

- `api-gateway` có `com.shopee.gateway.filter.TraceIdFilter` (`GlobalFilter`, `Ordered.HIGHEST_PRECEDENCE`): đọc header `X-Trace-Id` nếu client đã gửi sẵn, không thì tự sinh UUID, rồi gắn vào request trước khi forward xuống service phía sau, đồng thời lưu vào exchange attribute để `JwtAuthenticationFilter` dùng khi tự trả lỗi 401/403 (xem 21.2).
- `common` có `com.shopee.common.web.TraceIdFilter` (`OncePerRequestFilter`, `Order(HIGHEST_PRECEDENCE)`) chạy trong `user-service`/`product-service`: đọc header `X-Trace-Id` do Gateway gắn, đưa vào MDC (`MDC.put("traceId", ...)`) cho suốt vòng đời request rồi xoá ở khối `finally`. Nếu service bị gọi trực tiếp (bỏ qua Gateway) thì tự sinh UUID mới.
- Nhờ vậy `ApiMeta.traceId` trong mọi `ApiResponse` (thành công lẫn lỗi) đều có giá trị thật thay vì `null`, và cùng 1 `traceId` có thể dùng để nối log giữa Gateway và service xử lý request đó.

### 21.4. Không dùng JPA relationship

Ngoài phạm vi chuẩn response, `product-service` không dùng `@ManyToOne`/`@OneToMany` để lấy `Category` cho `Product` — `ProductService.list()` tự batch-fetch `Category` theo danh sách `categoryId` rồi ghép tay vào `PageData<ProductResponse>` qua hàm mapper của `PageData.from(Page<E>, Function<E,T>)`. Đây là quy ước riêng của dự án, không liên quan tới chuẩn response nhưng ảnh hưởng cách `PageData` được dựng trong tầng service.
