package com.beanchiuling.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Wrapper chuẩn cho TẤT CẢ response của API.
 *
 * 🎯 TẠI SAO cần wrapper thay vì trả thẳng data?
 *
 * Nếu không có wrapper, mỗi API trả khác nhau:
 *   - GET /users → { "id": 1, "name": "Bean" }
 *   - POST /auth/login → { "token": "..." }
 *   - Error → ??? (500 HTML page?)
 *
 * Với ApiResponse, Frontend luôn biết format cố định:
 *   Success: { "success": true, "data": {...} }
 *   Error:   { "success": false, "error": { "code": "USER_001", "message": "..." } }
 *
 * @JsonInclude(NON_NULL): Bỏ các field null khỏi JSON response
 *   → Nếu success thì không có field "error" trong JSON
 *   → Nếu error thì không có field "data" trong JSON
 *
 * 📌 CÁCH SỬ DỤNG trong Controller:
 *   return ResponseEntity.ok(ApiResponse.success(userDto));
 *   return ResponseEntity.status(400).body(ApiResponse.error("USER_001", "Not found"));
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    // ── Static Factory Methods ─────────────────────────

    /**
     * Trả về response thành công có data
     * VD: ApiResponse.success(userDto)
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * Trả về response thành công không có data (VD: delete, logout)
     * VD: ApiResponse.success()
     */
    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    /**
     * Trả về response lỗi
     * VD: ApiResponse.error("USER_001", "User not found")
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(new ErrorResponse(code, message))
                .build();
    }

    // ── Inner Class: Error Detail ──────────────────────

    @Getter
    public static class ErrorResponse {
        private final String code;
        private final String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
