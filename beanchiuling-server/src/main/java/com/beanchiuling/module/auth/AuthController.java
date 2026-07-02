package com.beanchiuling.module.auth;

import com.beanchiuling.common.response.ApiResponse;
import com.beanchiuling.module.auth.dto.LoginRequest;
import com.beanchiuling.module.auth.dto.RegisterRequest;
import com.beanchiuling.module.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller xử lý các request Authentication.
 *
 * 🎯 @RestController = @Controller + @ResponseBody
 *   - @Controller: Đây là Spring MVC controller
 *   - @ResponseBody: Tự serialize return value thành JSON
 *   → Mỗi method return object → Spring tự convert thành JSON response
 *
 * 🎯 @RequestMapping("/api/v1/auth"):
 *   Tất cả endpoint trong class này đều có prefix /api/v1/auth
 *   VD: @PostMapping("/register") → POST /api/v1/auth/register
 *
 * 🎯 Tầng Controller chỉ nên làm:
 *   1. Validate input (với @Valid)
 *   2. Gọi Service
 *   3. Wrap kết quả vào ResponseEntity
 *   KHÔNG nên có business logic ở đây!
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Đăng ký tài khoản mới.
     *
     * @Valid: Trigger Bean Validation trên RegisterRequest
     *   → Nếu vi phạm constraint → MethodArgumentNotValidException
     *   → GlobalExceptionHandler catch và trả về 400 với danh sách lỗi
     *
     * ResponseEntity.status(201): HTTP 201 Created
     *   (Quy ước REST: tạo resource mới thành công → 201, không phải 200)
     *
     * Request body mẫu:
     * {
     *   "email": "bean@example.com",
     *   "username": "beanchiuling",
     *   "password": "secret123",
     *   "displayName": "Bean 🫘"
     * }
     *
     * Response mẫu:
     * {
     *   "success": true,
     *   "data": {
     *     "accessToken": "eyJ...",
     *     "refreshToken": "eyJ...",
     *     "accessTokenExpiresAt": 1234567890000,
     *     "user": { "id": "uuid", "username": "beanchiuling", ... }
     *   }
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        TokenResponse tokenResponse = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)    // 201
                .body(ApiResponse.success(tokenResponse));
    }

    /**
     * POST /api/v1/auth/login
     * Đăng nhập với email + password.
     *
     * Response: 200 OK với tokens (giống register)
     * Nếu sai credentials: 401 Unauthorized
     *
     * Request body mẫu:
     * {
     *   "email": "bean@example.com",
     *   "password": "secret123"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        TokenResponse tokenResponse = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success(tokenResponse));    // 200
    }

    /**
     * POST /api/v1/auth/refresh
     * Lấy Access Token mới bằng Refresh Token.
     *
     * @RequestHeader: Đọc giá trị từ HTTP request header
     *   Client gửi: X-Refresh-Token: eyJ...
     *
     * Tại sao không dùng Authorization header?
     *   Authorization header được reserve cho Access Token.
     *   Dùng custom header X-Refresh-Token để tách biệt rõ ràng.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken) {

        TokenResponse tokenResponse = authService.refreshToken(refreshToken);

        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }
}
