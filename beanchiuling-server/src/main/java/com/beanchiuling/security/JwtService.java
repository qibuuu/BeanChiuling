package com.beanchiuling.security;

import com.beanchiuling.module.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service chịu trách nhiệm TẤT CẢ operations liên quan đến JWT.
 *
 * 🎯 JWT là gì? (JSON Web Token)
 *   Một chuỗi gồm 3 phần ngăn cách bởi dấu chấm:
 *
 *   HEADER.PAYLOAD.SIGNATURE
 *
 *   HEADER:    { "alg": "HS256", "typ": "JWT" }  ← Thuật toán dùng
 *   PAYLOAD:   { "sub": "user@email.com",         ← Dữ liệu (Claims)
 *                "userId": "uuid-...",
 *                "jti": "random-id",
 *                "iat": 1234567890,                ← Thời gian tạo
 *                "exp": 1234654290 }               ← Thời gian hết hạn
 *   SIGNATURE: HMACSHA256(base64(header) + "." + base64(payload), secretKey)
 *
 *   → Frontend lưu token này, gửi kèm mỗi request:
 *     Authorization: Bearer eyJhbGci...
 *
 *   → Backend verify signature bằng secretKey → nếu đúng → tin tưởng user
 *
 * 🎯 TẠI SAO JWT an toàn?
 *   Server KHÔNG lưu token → không cần database lookup mỗi request
 *   SIGNATURE được tạo bằng secretKey → không thể giả mạo nếu không có key
 *   Nếu ai đó sửa PAYLOAD → SIGNATURE không khớp → bị reject
 */
@Slf4j
@Service
public class JwtService {

    /**
     * @Value: Lấy giá trị từ application.yml
     * ${jwt.secret} → đọc field jwt.secret
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;           // 86400000ms = 24 giờ

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpiration;    // 604800000ms = 7 ngày

    // ──────────────────────────────────────────
    // GENERATE TOKENS
    // ──────────────────────────────────────────

    /**
     * Tạo Access Token cho user sau khi đăng nhập thành công.
     *
     * Claims (payload) bao gồm:
     *   - sub (subject): email của user → dùng để load user từ DB
     *   - userId: UUID của user → tránh query thêm lần nữa
     *   - username: tên hiển thị
     *   - type: "access" → phân biệt với refresh token
     *   - jti: JWT ID ngẫu nhiên → dùng để blacklist khi logout
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("type", "access");

        return buildToken(claims, user.getEmail(), jwtExpiration);
    }

    /**
     * Tạo Refresh Token — payload đơn giản hơn, chỉ dùng để đổi Access Token mới.
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", "refresh");

        return buildToken(claims, user.getEmail(), jwtRefreshExpiration);
    }

    /**
     * Build token chung — dùng bởi cả generateAccessToken và generateRefreshToken.
     *
     * Jwts.builder() API (JJWT library):
     *   .claims(extraClaims)  → Thêm custom claims vào payload
     *   .subject(subject)     → Set "sub" = email
     *   .issuedAt(now)        → Set "iat" = thời điểm tạo
     *   .expiration(expiry)   → Set "exp" = thời điểm hết hạn
     *   .id(UUID)             → Set "jti" = unique ID (dùng cho blacklist)
     *   .signWith(key)        → Ký bằng secretKey → tạo SIGNATURE
     *   .compact()            → Tạo chuỗi JWT cuối cùng
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString())   // jti — unique ID cho mỗi token
                .signWith(getSigningKey())
                .compact();
    }

    // ──────────────────────────────────────────
    // EXTRACT CLAIMS (Đọc thông tin từ token)
    // ──────────────────────────────────────────

    /** Lấy email (subject) từ token — dùng để load user từ DB */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Lấy JWT ID (jti) — dùng để kiểm tra token có bị blacklist không */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    /** Lấy userId từ custom claims */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    /** Lấy thời điểm hết hạn */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method dùng Function để extract bất kỳ claim nào.
     *
     * 🎯 Giải thích <T> và Function<Claims, T>:
     *   T là kiểu dữ liệu trả về (String, Date, Boolean...)
     *   Function<Claims, T> là một function nhận Claims → trả về T
     *   → Cho phép extract bất kỳ field nào mà không cần viết method riêng
     *
     *   VD: extractClaim(token, Claims::getSubject) → lấy subject (String)
     *       extractClaim(token, Claims::getExpiration) → lấy expiration (Date)
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ──────────────────────────────────────────
    // VALIDATE TOKEN
    // ──────────────────────────────────────────

    /**
     * Kiểm tra token có hợp lệ với user này không.
     * Điều kiện: email trong token phải khớp với email user + token chưa hết hạn.
     */
    public boolean isTokenValid(String token, User user) {
        final String email = extractEmail(token);
        return email.equals(user.getEmail()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Kiểm tra token có phải Access Token không (phân biệt với Refresh Token).
     */
    public boolean isAccessToken(String token) {
        String type = extractClaim(token, claims -> claims.get("type", String.class));
        return "access".equals(type);
    }

    /**
     * Thời gian còn lại của token (ms) — dùng để set TTL khi blacklist.
     */
    public long getRemainingValidity(String token) {
        Date expiration = extractExpiration(token);
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }

    // ──────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────

    /**
     * Parse token và lấy toàn bộ Claims.
     * Nếu token bị sửa đổi hoặc sai secretKey → JwtException được throw.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Chuyển đổi secretKey string → SecretKey object để JJWT dùng.
     *
     * 🎯 Keys.hmacShaKeyFor(): Tạo HMAC-SHA key từ byte array.
     *   Chuỗi secret phải đủ dài (>= 32 bytes = 256 bits cho HS256).
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
