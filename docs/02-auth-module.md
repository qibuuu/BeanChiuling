# Bước 2 — Auth Module (Xác thực & Phân quyền)

## Tổng quan

Auth Module là module phức tạp nhất nhưng cũng là nền tảng của mọi thứ khác. Không có Auth thì không có cách phân quyền cho các tính năng sau (Server, Channel, Message...).

**Luồng tổng thể:**
```
[Client] ──POST /auth/register──► [AuthController]
                                        │
                                   [AuthService]
                                   ├── Check email unique (UserRepository)
                                   ├── Hash password (BCrypt)
                                   ├── Save user (MySQL)
                                   └── Generate JWT (JwtService)
                                        │
                              { accessToken, refreshToken, user }

[Client gọi API sau đó]
Header: Authorization: Bearer <accessToken>
    ↓
[JwtAuthenticationFilter]  ← chạy trước mọi Controller
├── Extract token từ header
├── Validate signature & expiry
├── Load user từ DB
└── Set SecurityContext → request được coi là "authenticated"
    ↓
[Controller xử lý bình thường]
```

---

## Các file được tạo

### `module/user/entity/User.java`

**Là gì?** JPA Entity ánh xạ với bảng `users` trong MySQL.

**UUID làm Primary Key — tại sao?**

| | Auto-increment Long | UUID |
|---|---|---|
| URL | `/api/users/1` | `/api/users/a3b4c5d6-...` |
| Bảo mật | Dễ đoán, dễ enumerate | Không thể đoán |
| Performance | Insert nhanh hơn | Nhẹ hơn một chút |
| Distributed | Conflict khi multi-instance | Globally unique |

Với hệ thống như Discord (nhiều server), UUID an toàn hơn.

**Tại sao `implements UserDetails`?**

Spring Security cần biết cách lấy thông tin user để authenticate. Thay vì tạo class wrapper riêng, ta implement thẳng trên Entity:

```java
// Spring Security gọi những method này khi authenticate:
getUsername()  → trả về EMAIL (ta dùng email để login, không phải username)
getPassword()  → trả về password HASH (BCrypt)
isEnabled()    → true (account active)
getAuthorities() → List.of() (rỗng — ta dùng bitfield permission system riêng)
```

⚠️ **Lưu ý quan trọng:** `getUsername()` bị override để trả về **email** (không phải username thật). Khi cần username thật trong response, gọi `getUsernameValue()`.

**`@CreationTimestamp` và `@UpdateTimestamp`:**
Hibernate tự động set giá trị — không cần viết code:
- `createdAt`: Set 1 lần khi INSERT, không bao giờ thay đổi (`updatable = false`)
- `updatedAt`: Cập nhật mỗi lần UPDATE

**Lombok annotations:**
```java
@Getter  → public String getId() { return id; }  // Tự sinh
@Setter  → public void setId(String id) { this.id = id; }  // Tự sinh
@Builder → User.builder().email("...").password("...").build()  // Builder pattern
@NoArgsConstructor → User() {}  // JPA yêu cầu constructor không tham số
@AllArgsConstructor → User(id, username, ...) {}  // Builder cần
```

---

### `module/user/UserRepository.java`

**Là gì?** Interface để giao tiếp với MySQL. Spring Data JPA tự sinh implementation.

**Naming Convention — Spring tự parse tên method thành SQL:**

```java
findByEmail(email)
 ↓ Spring hiểu
SELECT * FROM users WHERE email = ?

existsByUsername(username)
 ↓ Spring hiểu
SELECT COUNT(*) > 0 FROM users WHERE username = ?
```

Các keyword được hỗ trợ: `findBy`, `existsBy`, `countBy`, `deleteBy`, `And`, `Or`, `Between`, `LessThan`, `GreaterThan`, `Like`, `OrderBy`...

**`Optional<User>` vs `User`:**
```java
Optional<User> findByEmail(String email);

// Buộc caller phải xử lý trường hợp không tìm thấy:
userRepository.findByEmail(email)
    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
// → Không bao giờ NullPointerException
```

---

### `module/auth/dto/RegisterRequest.java`

**Là gì?** Data Transfer Object — object nhận dữ liệu từ request body.

**Tại sao không nhận thẳng Entity `User`?**

Nếu nhận thẳng `User`, client có thể gửi:
```json
{
  "id": "custom-uuid",
  "createdAt": "2020-01-01",
  "password": "plain text"
}
```
→ Mass assignment vulnerability!

DTO chỉ expose đúng fields cần thiết.

**Bean Validation Annotations:**

```java
@NotBlank  → Không null, không rỗng, không chỉ spaces
@Email     → Phải có format a@b.c
@Size(min, max) → Giới hạn độ dài
@Pattern(regexp) → Phải khớp regex

// Nếu vi phạm → MethodArgumentNotValidException
// → GlobalExceptionHandler xử lý → HTTP 400 + danh sách lỗi
```

**Regex cho username:**
```
^[a-z0-9_]+$
^  = bắt đầu chuỗi
[a-z0-9_] = chỉ chứa: chữ thường, số, dấu gạch dưới
+  = một hoặc nhiều ký tự
$  = kết thúc chuỗi
```

---

### `module/auth/dto/TokenResponse.java`

**Là gì?** DTO trả về sau đăng nhập/đăng ký thành công.

**Access Token vs Refresh Token:**

```
Access Token (24h)
├── Ngắn hạn → nếu bị lộ, thiệt hại giới hạn trong 24h
├── Gửi trong mọi API request: Authorization: Bearer <token>
└── Khi hết hạn → dùng Refresh Token để lấy cái mới

Refresh Token (7 ngày)
├── Dài hạn → lưu an toàn hơn (HttpOnly cookie)
├── CHỈ gửi đến endpoint /auth/refresh
└── Khi hết hạn → bắt buộc đăng nhập lại
```

**Inner class `UserInfo`:**
Trả về thông tin cơ bản của user ngay trong response đăng nhập — Frontend không cần gọi thêm GET /users/@me để lấy tên, avatar.

---

### `security/JwtService.java`

**Là gì?** Service xử lý tất cả operations liên quan đến JWT.

**JWT gồm 3 phần:**
```
eyJhbGciOiJIUzUxMiJ9  .  eyJ1c2VySWQiOiIuLi4ifQ  .  9IbB_CJO...
        ↓                          ↓                        ↓
    HEADER (base64)          PAYLOAD (base64)          SIGNATURE
{ "alg": "HS512" }    { "sub": "bean@email.com",   HMACSHA512(
                         "userId": "uuid",           header + payload,
                         "type": "access",           secretKey
                         "iat": 1234567890,          )
                         "exp": 1234654290,
                         "jti": "random-uuid" }
```

**Tại sao JWT an toàn?**
- SIGNATURE được tạo bằng `secretKey` — chỉ server biết
- Nếu ai sửa PAYLOAD → SIGNATURE không còn khớp → bị reject
- Server **không cần lưu token** → stateless, dễ scale

**`jti` (JWT ID) dùng cho gì?**
Mỗi token có một UUID ngẫu nhiên. Khi user logout, server lưu `jti` vào Redis blacklist. Các request sau với token đó sẽ bị reject dù signature vẫn hợp lệ.

**Generic method `extractClaim<T>`:**
```java
// Thay vì viết riêng từng method:
String extractEmail(token)  { return claims.getSubject(); }
Date extractExp(token)      { return claims.getExpiration(); }

// Dùng Function để tái sử dụng logic parse:
<T> T extractClaim(token, Function<Claims, T> resolver)

// Gọi:
extractClaim(token, Claims::getSubject)     // → String
extractClaim(token, Claims::getExpiration)  // → Date
```

---

### `security/JwtAuthenticationFilter.java`

**Là gì?** Filter chặn mọi request để kiểm tra JWT.

**Filter Chain trong Spring:**
```
HTTP Request
    ↓
[DisableEncodeUrlFilter]
[CorsFilter]
[JwtAuthenticationFilter]  ← File này
[AnonymousAuthenticationFilter]
[AuthorizationFilter]
    ↓
Controller
```

**`OncePerRequestFilter` — tại sao "Once"?**
Trong một số trường hợp (forward, include), filter có thể bị gọi nhiều lần. `OncePerRequestFilter` đảm bảo logic chỉ chạy đúng 1 lần.

**Flow xử lý:**
```
1. Đọc header: "Authorization: Bearer eyJ..."
2. Tách lấy token (substring sau "Bearer ")
3. extractEmail(token) → "bean@email.com"
4. Load User từ DB bằng email
5. isTokenValid(token, user) → true/false
6. Nếu valid → tạo Authentication object
7. Set vào SecurityContextHolder
8. filterChain.doFilter() → tiếp tục
```

**SecurityContextHolder:**
Lưu Authentication trong `ThreadLocal` — mỗi request chạy trong một thread riêng, nên thông tin user không bị lẫn giữa các request đồng thời.

---

### `security/UserDetailsServiceImpl.java`

**Là gì?** Cầu nối giữa Spring Security và database.

Spring Security không biết ta dùng MySQL hay file hay LDAP. Nó chỉ gọi:
```java
userDetailsService.loadUserByUsername("bean@email.com")
```
→ Ta implement: query MySQL → trả về User entity.

**Tại sao tách thành service riêng?**
Để tránh **circular dependency**:
- `SecurityConfig` cần `UserDetailsService`
- `UserDetailsService` cần `UserRepository`
- Nếu inject trực tiếp repository vào `SecurityConfig` → Spring khó giải quyết dependency graph

---

### `config/SecurityConfig.java`

**Là gì?** Cấu hình toàn bộ Spring Security.

**`SecurityFilterChain` — các quy tắc quan trọng:**

```java
// 1. Tắt CSRF — vì ta dùng JWT stateless, không dùng session/cookie
.csrf(disable)

// 2. Endpoint nào public, endpoint nào cần auth:
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/**").permitAll()  // Public
    .anyRequest().authenticated()                     // Còn lại cần JWT
)

// 3. Stateless — không tạo HTTP Session
.sessionManagement(STATELESS)

// 4. Đặt JwtFilter trước filter xử lý username/password
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
```

**`BCryptPasswordEncoder` — tại sao BCrypt?**
```
"123456" → BCrypt → "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68..."

Đặc điểm:
├── Hash một chiều (không giải mã được)
├── Tự thêm "salt" ngẫu nhiên → cùng password, hash khác nhau
├── strength=10 → 2^10 = 1024 vòng lặp → chậm chủ ý để chống brute force
└── 60 ký tự cố định
```

**CORS Configuration:**
Browser block request từ `localhost:3000` (Frontend) đến `localhost:8080` (Backend) nếu không cấu hình. `CorsConfigurationSource` định nghĩa origin nào được phép và với method/header nào.

---

### `module/auth/AuthService.java`

**Là gì?** Xử lý business logic của authentication.

**`@Transactional` trong `register()`:**
```java
@Transactional
public TokenResponse register(RegisterRequest request) {
    // Bước 1: check email unique → OK
    // Bước 2: check username unique → OK
    // Bước 3: save user → THÀNH CÔNG
    // Bước 4: generateToken → throw SomeException?
    //   → ROLLBACK: user record bị xóa khỏi DB
    //   → DB sạch sẽ, không có zombie records
}
```

**User Enumeration Attack — tại sao không phân biệt "email sai" và "password sai"?**
```
❌ Tệ:
"Email không tồn tại" → Hacker biết email nào đã đăng ký

✅ Đúng (BeanChiuling):
"Invalid email or password" → Hacker không biết gì
```

**`AuthenticationManager.authenticate()`:**
Spring Security tự làm tất cả:
1. Gọi `UserDetailsService.loadUserByUsername(email)`
2. So sánh: `passwordEncoder.matches(rawPwd, hashedPwd)`
3. Nếu đúng → trả về `Authentication`
4. Nếu sai → throw `BadCredentialsException`

---

### `module/auth/AuthController.java`

**Là gì?** REST Controller — tầng tiếp nhận request, gọi service, trả response.

**Controller KHÔNG nên có business logic:**
```java
// ✅ Đúng — Controller chỉ là "thư ký"
@PostMapping("/register")
public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest req) {
    return ResponseEntity.status(201).body(ApiResponse.success(authService.register(req)));
}

// ❌ Sai — Business logic trong Controller
@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
    if (userRepo.existsByEmail(req.getEmail())) { ... }  // Không nên ở đây
    ...
}
```

**HTTP Status Code đúng chuẩn REST:**
- `201 Created` khi đăng ký thành công (tạo resource mới)
- `200 OK` khi đăng nhập (không tạo resource mới)
- `401 Unauthorized` khi sai credentials
- `400 Bad Request` khi validation thất bại

**`@RequestHeader("X-Refresh-Token")`:**
Dùng custom header thay vì `Authorization` để tránh nhầm lẫn — `Authorization` header dành cho Access Token.
