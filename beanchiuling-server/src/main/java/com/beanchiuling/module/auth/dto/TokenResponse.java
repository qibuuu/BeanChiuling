package com.beanchiuling.module.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO trả về sau khi đăng nhập hoặc đăng ký thành công.
 *
 * 🎯 Access Token vs Refresh Token:
 *
 *   Access Token (ngắn hạn — 24h):
 *     - Gửi trong mỗi API request header: Authorization: Bearer <token>
 *     - Nếu bị lộ → hacker chỉ có tối đa 24h
 *
 *   Refresh Token (dài hạn — 7 ngày):
 *     - Lưu an toàn (HttpOnly cookie hoặc secure storage)
 *     - Dùng để lấy Access Token mới khi hết hạn
 *     - KHÔNG gửi trong mỗi request thường
 *     - Nếu bị lộ → logout và revoke ngay
 *
 *   Flow:
 *     Login → nhận cả 2 token
 *     24h sau: Access Token hết hạn
 *     → Dùng Refresh Token gọi POST /auth/refresh
 *     → Nhận Access Token mới (không cần đăng nhập lại)
 *     7 ngày sau: Refresh Token hết hạn → bắt buộc đăng nhập lại
 */
@Getter
@Builder
public class TokenResponse {

    private String accessToken;
    private String refreshToken;

    /** Thời gian hết hạn của Access Token (milliseconds từ epoch) */
    private long accessTokenExpiresAt;

    /** Thông tin cơ bản của user để Frontend dùng ngay */
    private UserInfo user;

    @Getter
    @Builder
    public static class UserInfo {
        private String id;
        private String username;
        private String email;
        private String displayName;
        private String avatarUrl;
    }
}
