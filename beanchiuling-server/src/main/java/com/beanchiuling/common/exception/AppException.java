package com.beanchiuling.common.exception;

import lombok.Getter;

/**
 * Custom Runtime Exception dùng trong toàn bộ ứng dụng.
 *
 * 🎯 TẠI SAO tạo custom exception thay vì dùng RuntimeException thẳng?
 *
 * Khi bạn throw RuntimeException("User not found"), bạn mất thông tin:
 *   - HTTP status nên trả về là gì? (404 hay 500?)
 *   - Mã lỗi cụ thể cho Frontend là gì?
 *
 * Với AppException, chỉ cần: throw new AppException(ErrorCode.USER_NOT_FOUND)
 * → GlobalExceptionHandler sẽ tự xử lý, trả về đúng HTTP status + message.
 *
 * 📌 CÁCH SỬ DỤNG:
 *   throw new AppException(ErrorCode.USER_NOT_FOUND);
 *   throw new AppException(ErrorCode.SERVER_FORBIDDEN);
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // Overload: cho phép truyền custom message nếu cần
    public AppException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
