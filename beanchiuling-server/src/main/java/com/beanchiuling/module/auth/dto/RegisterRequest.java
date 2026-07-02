package com.beanchiuling.module.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO nhận dữ liệu từ request body khi đăng ký.
 *
 * 🎯 TẠI SAO cần DTO thay vì nhận thẳng Entity User?
 *   Nếu nhận thẳng User entity, client có thể gửi lên bất kỳ field nào,
 *   kể cả "id", "createdAt", "roles"... → rất nguy hiểm!
 *   DTO chỉ nhận đúng những field bạn cho phép.
 *
 * 🎯 Bean Validation Annotations:
 *   @NotBlank  → Không được null, không được là chuỗi rỗng hoặc chỉ có space
 *   @Email     → Phải đúng format email (có @ và domain)
 *   @Size      → Giới hạn độ dài min/max
 *   @Pattern   → Phải khớp với regex
 *
 *   Khi controller có @Valid trước @RequestBody, Spring tự validate
 *   → Nếu sai → MethodArgumentNotValidException → GlobalExceptionHandler bắt
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    /**
     * Username: chữ thường, số và dấu gạch dưới, 3-50 ký tự.
     * Regex: ^[a-z0-9_]{3,50}$
     *   ^ = bắt đầu chuỗi
     *   [a-z0-9_] = chỉ chứa a-z, 0-9, dấu _
     *   {3,50} = độ dài từ 3 đến 50
     *   $ = kết thúc chuỗi
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Username can only contain lowercase letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    /** Tên hiển thị (optional, nếu không nhập thì dùng username) */
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;
}
