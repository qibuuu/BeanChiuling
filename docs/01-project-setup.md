# Bước 1 — Project Setup & Foundation

## Tổng quan

Bước này thiết lập **nền tảng kiến trúc** cho toàn bộ dự án BeanChiuling trước khi viết bất kỳ tính năng nào. Mục tiêu là tạo ra 3 thứ: cấu trúc package rõ ràng, hệ thống xử lý lỗi thống nhất, và format response nhất quán.

---

## Các file được tạo

### `pom.xml`

**Là gì?** File cấu hình Maven — trình quản lý dependency của Java (giống `package.json` trong Node.js).

**Dependencies quan trọng:**

| Dependency | Tác dụng |
|---|---|
| `spring-boot-starter-web` | REST API framework (Tomcat server, Jackson JSON) |
| `spring-boot-starter-data-jpa` | Kết nối MySQL thông qua Hibernate ORM |
| `spring-boot-starter-data-mongodb` | Kết nối MongoDB để lưu tin nhắn |
| `spring-boot-starter-data-redis` | Kết nối Redis để cache và quản lý presence |
| `spring-boot-starter-websocket` | WebSocket server (STOMP protocol) |
| `spring-boot-starter-security` | Bảo mật: auth, authorization |
| `spring-boot-starter-validation` | Bean Validation (validate request body) |
| `mysql-connector-j` | JDBC driver cho MySQL |
| `lombok` | Tạo getter/setter/constructor/builder bằng annotation |
| `jjwt-api/impl/jackson` | Thư viện tạo và validate JWT token |
| `spring-boot-starter-amqp` | Kết nối RabbitMQ (message broker) |
| `mapstruct` | Tự động convert giữa Entity ↔ DTO |

**Tại sao cần Maven Wrapper (`mvnw.cmd`)?**
Thay vì yêu cầu tất cả developer cài Maven toàn cục, Wrapper tự tải đúng phiên bản Maven về máy local. Chỉ cần Java là đủ để chạy project.

---

### `src/main/resources/application.yml`

**Là gì?** File cấu hình chính của Spring Boot. Thay thế cho `application.properties` (YAML dễ đọc hơn vì hỗ trợ nesting).

**Cấu trúc cấu hình:**

```
application.yml (template — commit lên GitHub, KHÔNG có secrets thật)
    ↓ override bởi
application-local.yml (secrets thật — gitignored, chỉ có trên máy local)
```

**Pattern `${ENV_VAR:default_value}`:**
```yaml
password: ${DB_PASSWORD:your_mysql_password}
```
- Nếu có biến môi trường `DB_PASSWORD` → dùng giá trị đó
- Nếu không → dùng `your_mysql_password` làm fallback
- **Mục đích:** Deploy lên server thật chỉ cần set env var, không cần sửa file

**Tại sao `open-in-view: false`?**
Spring JPA mặc định giữ database connection mở suốt quá trình render view — gây ra N+1 query problem và waste connection pool. Tắt đi để kiểm soát tốt hơn.

---

### `BeanChiulingApplication.java`

**Là gì?** Entry point — điểm khởi động của toàn bộ ứng dụng Spring Boot.

**`@SpringBootApplication` gồm 3 annotation gộp lại:**

```
@SpringBootApplication
    ├── @Configuration         → Class này định nghĩa Spring Beans
    ├── @EnableAutoConfiguration → Tự cấu hình dựa theo dependencies trong pom.xml
    │     (Có MySQL driver → tự tạo DataSource bean)
    │     (Có Web → tự khởi động Tomcat)
    └── @ComponentScan         → Quét và đăng ký tất cả @Component, @Service,
                                 @Repository, @Controller trong package này
```

**`SpringApplication.run()`:** Khởi động IoC Container (Spring ApplicationContext), sau đó start Tomcat server.

---

### `common/exception/ErrorCode.java`

**Là gì?** Enum tập trung toàn bộ mã lỗi của ứng dụng.

**Tại sao dùng Enum thay vì hardcode string?**

❌ **Cách tệ:**
```java
throw new RuntimeException("User not found");
// Sau 3 tháng bạn viết:
throw new RuntimeException("user not found"); // Typo khác!
// Frontend phải check cả 2 string
```

✅ **Cách đúng (Enum):**
```java
throw new AppException(ErrorCode.USER_NOT_FOUND);
// Compile-time check → không thể sai tên
// Một chỗ thay đổi → áp dụng khắp nơi
```

**Cấu trúc mỗi ErrorCode:**
```java
USER_NOT_FOUND(404, "USER_001", "User not found", HttpStatus.NOT_FOUND)
               ↑      ↑              ↑                    ↑
          HTTP code  Error ID    Message mặc định    Spring HttpStatus
```

**Tại sao có cả `statusCode` (int) lẫn `HttpStatus` (enum)?**
- `HttpStatus` dùng khi build `ResponseEntity`
- `statusCode` (int) dùng khi cần log hoặc serialize ra JSON

---

### `common/exception/AppException.java`

**Là gì?** Custom unchecked exception duy nhất dùng trong toàn bộ business logic.

**Tại sao không dùng thẳng `RuntimeException`?**

`RuntimeException` không mang theo context:
```java
throw new RuntimeException("User not found");
// GlobalExceptionHandler không biết nên trả về 404 hay 500?
```

`AppException` mang theo `ErrorCode`:
```java
throw new AppException(ErrorCode.USER_NOT_FOUND);
// GlobalExceptionHandler biết ngay: HTTP 404, message "User not found"
```

**Unchecked vs Checked Exception:**
- **Checked** (`IOException`, `SQLException`): Bắt buộc phải `try-catch` hoặc khai báo `throws` → verbose
- **Unchecked** (`RuntimeException`): Không cần khai báo → code gọn hơn, phù hợp cho business errors

---

### `common/exception/GlobalExceptionHandler.java`

**Là gì?** Interceptor bắt tất cả exception từ mọi Controller và chuyển thành HTTP response chuẩn.

**`@RestControllerAdvice` hoạt động thế nào?**

```
Request → Controller → throw AppException
                              ↓
                    GlobalExceptionHandler
                    (bắt, xử lý, trả về JSON)
                              ↓
                    Response: { success: false, error: {...} }
```

Không có handler này, Spring sẽ trả về HTML error page mặc định (không phù hợp cho REST API).

**Thứ tự ưu tiên của `@ExceptionHandler`:**
Spring chọn handler **cụ thể nhất** trước:
1. `AppException` (cụ thể nhất — custom của ta)
2. `MethodArgumentNotValidException` (validation errors)
3. `BadCredentialsException` (auth errors)
4. `Exception` (fallback chung nhất — "lưới cuối")

**Tại sao `Exception` handler không log message mà log stack trace?**
- Message có thể là thông tin nhạy cảm (SQL error, internal path...)
- Stack trace chỉ thấy ở server log, không bị lộ ra client

---

### `common/response/ApiResponse.java`

**Là gì?** Generic wrapper cho TẤT CẢ response của API.

**Tại sao cần wrapper?**

Nếu không có wrapper:
```
GET /users/1     → { "id": 1, "name": "Bean" }
GET /servers     → [{ "id": "uuid" }, ...]
POST /auth/login → { "token": "..." }
Error 404        → ??? (HTML page? plain text?)
```

Với `ApiResponse<T>`, Frontend luôn biết format:
```json
// Success:
{ "success": true, "data": { ... } }

// Error:
{ "success": false, "error": { "code": "USER_001", "message": "..." } }
```

**`@JsonInclude(NON_NULL)`:**
Loại bỏ field `null` khỏi JSON. Nếu success thì `error` field không xuất hiện trong JSON response, giữ response gọn.

**Generic `<T>` là gì?**
Cho phép `data` là bất kỳ kiểu nào:
- `ApiResponse<UserDto>` → data là một UserDto
- `ApiResponse<List<ServerDto>>` → data là list
- `ApiResponse<Void>` → không có data (VD: delete thành công)

---

## Luồng hoạt động khi có lỗi

```
POST /api/v1/auth/register (email rỗng)
    ↓
AuthController.register(@Valid @RequestBody ...)
    ↓ validation fail
MethodArgumentNotValidException thrown
    ↓
GlobalExceptionHandler.handleValidationException()
    ↓
ResponseEntity(400, ApiResponse.error("ERR_400", { email: "must not be blank" }))
    ↓
{ "success": false, "data": { "email": "must not be blank" } }
```
