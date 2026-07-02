package com.beanchiuling.module.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Entity đại diện cho bảng `users` trong MySQL.
 *
 * 🎯 TẠI SAO implement UserDetails?
 *   Spring Security cần interface UserDetails để biết:
 *   - Username (dùng để login) là gì?
 *   - Password (đã hash) là gì?
 *   - Account có bị khóa / hết hạn không?
 *   Bằng cách implement UserDetails ngay trên Entity, ta không cần tạo
 *   class UserDetails riêng — gọn hơn và Spring Security tự hiểu.
 *
 * 🎯 @Entity + @Table: Ánh xạ class này với bảng "users" trong database.
 *
 * 🎯 Annotations của Lombok:
 *   @Getter     → Tự sinh getter cho tất cả field
 *   @Setter     → Tự sinh setter
 *   @Builder    → Tạo Builder pattern: User.builder().username("bean").build()
 *   @NoArgsConstructor → Constructor không tham số (JPA yêu cầu)
 *   @AllArgsConstructor → Constructor đầy đủ tham số (Builder cần)
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    /**
     * Primary Key dùng UUID thay vì Long auto-increment.
     *
     * 🎯 TẠI SAO UUID?
     *   - Long: 1, 2, 3, 4... → Người dùng có thể đoán ID → bảo mật kém
     *     VD: /api/users/1 → đổi thành /api/users/2 → xem thông tin người khác?
     *   - UUID: "a3b4c5d6-..." → Không đoán được, an toàn hơn
     *
     * CHAR(36): MySQL không có native UUID type nên dùng CHAR(36) để lưu UUID string.
     * @GeneratedValue(UuidGenerator): Hibernate tự tạo UUID mới trước khi INSERT.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    /**
     * Username: tên đăng nhập, duy nhất trong hệ thống.
     * VD: "beanchiuling", "quang2807"
     * Khác với displayName (tên hiển thị có thể trùng và thay đổi được)
     */
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    /**
     * Lưu PASSWORD ĐÃ HASH — KHÔNG BAO GIỜ lưu plain text password!
     * Spring Security sẽ tự hash bằng BCrypt trước khi lưu.
     * VD: "123456" → "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
     */
    @Column(name = "password_hash", nullable = false)
    private String password;

    /**
     * Tên hiển thị (display name) — có thể trùng, có thể thay đổi.
     * VD: "Bean 🫘", "Quang Dev"
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /** URL avatar, lưu trên Cloudinary CDN */
    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    /** Mô tả ngắn về bản thân */
    @Column(name = "about_me", columnDefinition = "TEXT")
    private String aboutMe;

    /**
     * Timestamps tự động:
     * @CreationTimestamp: Hibernate tự set khi INSERT (không cần code thêm)
     * @UpdateTimestamp: Hibernate tự set khi UPDATE
     * updatable = false: createdAt không bao giờ bị update sau khi tạo
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ──────────────────────────────────────────
    // UserDetails interface implementation
    // Spring Security sẽ gọi các method này
    // ──────────────────────────────────────────

    /**
     * Trả về danh sách quyền (roles/authorities) của user.
     * BeanChiuling dùng Permission System riêng (bitfield) thay vì Spring roles,
     * nên trả về list rỗng ở đây.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    /**
     * Spring Security dùng method này để lấy password hash khi authenticate.
     * Field của ta tên là "password" nên method này đã đúng tên.
     */
    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * Spring Security dùng method này để lấy "username" khi authenticate.
     * Ta dùng EMAIL để đăng nhập (giống Discord dùng email).
     *
     * 🎯 Quan trọng: Return email ở đây để JwtAuthFilter có thể
     *    load user bằng email từ JWT token subject.
     */
    @Override
    public String getUsername() {
        return this.email;
    }

    /**
     * Lấy username thật (tên đăng nhập, VD: "beanchiuling").
     * Dùng khi cần hiển thị username trong response, khác với getUsername()
     * của Spring Security (trả về email).
     */
    public String getUsernameValue() {
        return this.username;
    }

    // Các method sau trả về true để không block account
    // Sau này có thể thêm logic: email chưa verify → isEnabled() = false
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
