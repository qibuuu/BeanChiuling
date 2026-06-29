package com.beanchiuling.common.exception;

import com.beanchiuling.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Trung tâm xử lý tất cả exceptions xảy ra trong ứng dụng.
 *
 * 🎯 @RestControllerAdvice hoạt động thế nào?
 *   - Interceptor chặn TẤT CẢ exception thrown từ mọi @RestController
 *   - Thay vì để Spring trả về HTML error page mặc định, chúng ta tự xử lý
 *   - Mỗi @ExceptionHandler xử lý một loại exception cụ thể
 *
 * 🎯 @Slf4j (Lombok): Tự tạo biến logger để ghi log, tương đương:
 *   private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
 *
 * THỨ TỰ QUAN TRỌNG: Spring sẽ chọn handler cụ thể nhất trước
 *   AppException (cụ thể) → MethodArgumentNotValidException → Exception (chung nhất)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý AppException — lỗi business logic do chúng ta tự throw.
     *
     * VD: throw new AppException(ErrorCode.USER_NOT_FOUND)
     * → Trả về: HTTP 404, { success: false, error: { code: "USER_001", message: "User not found" } }
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("AppException: {} - {}", errorCode.getErrorCode(), ex.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getErrorCode(), ex.getMessage()));
    }

    /**
     * Xử lý lỗi Validation — khi request body không hợp lệ.
     *
     * VD: @Valid RegisterRequest có @NotBlank email, user gửi email rỗng
     * → Trả về: HTTP 400, { success: false, error: { code: "ERR_400", message: {email: "must not be blank"} } }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        // Lấy tất cả field errors và gom vào 1 Map
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .data(errors)  // Trả về Map lỗi trong "data" để Frontend biết field nào sai
                        .build());
    }

    /**
     * Xử lý lỗi xác thực sai (sai email/password).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(401)
                .body(ApiResponse.error(
                        ErrorCode.INVALID_CREDENTIALS.getErrorCode(),
                        ErrorCode.INVALID_CREDENTIALS.getMessage()
                ));
    }

    /**
     * Xử lý lỗi phân quyền (403 Forbidden) từ Spring Security.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(403)
                .body(ApiResponse.error("AUTH_FORBIDDEN", "You don't have permission to perform this action"));
    }

    /**
     * "Lưới cuối" — bắt tất cả exception không được xử lý ở trên.
     * Luôn phải có để tránh lộ stack trace ra ngoài.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        // Log đầy đủ stack trace để debug (chỉ thấy ở server logs)
        log.error("Unhandled exception", ex);

        // Trả về thông báo chung chung, KHÔNG lộ chi tiết lỗi cho client
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_ERROR.getErrorCode(),
                        ErrorCode.INTERNAL_ERROR.getMessage()
                ));
    }
}
