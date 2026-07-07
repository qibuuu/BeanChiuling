package com.beanchiuling.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enum tập trung TẤT CẢ các mã lỗi của ứng dụng.
 *
 * 🎯 TẠI SAO dùng Enum thay vì hardcode string?
 *   - Tránh lỗi typo (lỗi đánh máy) khi dùng đi dùng lại
 *   - Dễ dàng tìm kiếm tất cả chỗ dùng một loại lỗi cụ thể
 *   - Thống nhất HTTP status code tương ứng với từng loại lỗi
 *   - Frontend có thể dùng errorCode để hiển thị thông báo phù hợp ngôn ngữ
 *
 * Convention đặt tên: DOMAIN_DESCRIPTION
 * VD: USER_NOT_FOUND, SERVER_FORBIDDEN, MESSAGE_TOO_LONG
 */
@Getter
public enum ErrorCode {

    // ── GENERIC ──────────────────────────────────────
    INTERNAL_ERROR(500, "ERR_500", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_FAILED(400, "ERR_400", "Validation failed", HttpStatus.BAD_REQUEST),

    // ── AUTH ──────────────────────────────────────────
    INVALID_CREDENTIALS(401, "AUTH_001", "Invalid email or password", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(401, "AUTH_002", "Access token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(401, "AUTH_003", "Token is invalid or malformed", HttpStatus.UNAUTHORIZED),
    TOKEN_BLACKLISTED(401, "AUTH_004", "Token has been revoked", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(401, "AUTH_005", "Authentication required", HttpStatus.UNAUTHORIZED),

    // ── USER ──────────────────────────────────────────
    USER_NOT_FOUND(404, "USER_001", "User not found", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS(409, "USER_002", "Email is already registered", HttpStatus.CONFLICT),
    USERNAME_ALREADY_EXISTS(409, "USER_003", "Username is already taken", HttpStatus.CONFLICT),

    // ── SERVER ────────────────────────────────────────
    SERVER_NOT_FOUND(404, "SRV_001", "Server not found", HttpStatus.NOT_FOUND),
    SERVER_FORBIDDEN(403, "SRV_002", "You don't have permission to access this server", HttpStatus.FORBIDDEN),
    SERVER_ALREADY_MEMBER(409, "SRV_003", "You are already a member of this server", HttpStatus.CONFLICT),
    SERVER_FULL(400, "SRV_004", "Server has reached maximum member capacity", HttpStatus.BAD_REQUEST),

    // ── INVITE ────────────────────────────────────────
    INVITE_NOT_FOUND(404, "INV_001", "Invite link not found or expired", HttpStatus.NOT_FOUND),
    INVITE_EXPIRED(400, "INV_002", "Invite link has expired", HttpStatus.BAD_REQUEST),
    INVITE_MAX_USES_REACHED(400, "INV_003", "Invite link has reached maximum uses", HttpStatus.BAD_REQUEST),

    // ── CHANNEL ───────────────────────────────────────
    CHANNEL_NOT_FOUND(404, "CH_001", "Channel not found", HttpStatus.NOT_FOUND),
    CHANNEL_FORBIDDEN(403, "CH_002", "You don't have permission to access this channel", HttpStatus.FORBIDDEN),

    // ── MESSAGE ───────────────────────────────────────
    MESSAGE_NOT_FOUND(404, "MSG_001", "Message not found", HttpStatus.NOT_FOUND),
    MESSAGE_FORBIDDEN(403, "MSG_002", "You cannot modify this message", HttpStatus.FORBIDDEN),
    MESSAGE_TOO_LONG(400, "MSG_003", "Message content exceeds maximum length of 2000 characters", HttpStatus.BAD_REQUEST),
    SEND_MESSAGE_FORBIDDEN(403, "MSG_004", "You don't have permission to send messages in this channel", HttpStatus.FORBIDDEN),

    // ── ROLE ──────────────────────────────────────────
    ROLE_NOT_FOUND(404, "ROLE_001", "Role not found", HttpStatus.NOT_FOUND),
    ROLE_FORBIDDEN(403, "ROLE_002", "Cannot modify a role with higher position than yours", HttpStatus.FORBIDDEN),

    // ── FILE ──────────────────────────────────────────
    FILE_TOO_LARGE(400, "FILE_001", "File size exceeds the 8MB limit", HttpStatus.BAD_REQUEST),
    FILE_TYPE_NOT_SUPPORTED(400, "FILE_002", "File type is not supported", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(500, "FILE_003", "Failed to upload file to storage", HttpStatus.INTERNAL_SERVER_ERROR),

    // -- FRIEND
    FRIEND_NOT_FOUND(404, "FR_001", "Friend request not found", HttpStatus.NOT_FOUND),
    FRIEND_ALREADY_EXISTS(409, "FR_002", "Already friends or request already sent", HttpStatus.CONFLICT),
    FRIEND_REQUEST_SELF(400, "FR_003", "Cannot send a friend request to yourself", HttpStatus.BAD_REQUEST);

    // ──────────────────────────────────────────────────
    private final int statusCode;      // HTTP status code số
    private final String errorCode;    // Mã lỗi cho Frontend dùng (ổn định, không đổi)
    private final String message;      // Thông báo mặc định
    private final HttpStatus httpStatus; // HTTP Status enum

    ErrorCode(int statusCode, String errorCode, String message, HttpStatus httpStatus) {
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
