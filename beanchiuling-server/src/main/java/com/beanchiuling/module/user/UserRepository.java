package com.beanchiuling.module.user;

import com.beanchiuling.module.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository để tương tác với bảng `users` trong MySQL.
 *
 * 🎯 JpaRepository<User, String> cho phép gì?
 *   Spring Data JPA tự sinh implementation cho các method CRUD:
 *   - save(user)           → INSERT hoặc UPDATE
 *   - findById(id)         → SELECT WHERE id = ?
 *   - findAll()            → SELECT * FROM users
 *   - delete(user)         → DELETE
 *   - count()              → SELECT COUNT(*)
 *   ... và hàng chục method khác — KHÔNG cần viết SQL!
 *
 * 🎯 Naming Convention của Spring Data:
 *   findBy + {TênField} + {Điều kiện}
 *   → Spring tự hiểu và tạo query phù hợp:
 *
 *   findByEmail(email)           → SELECT * FROM users WHERE email = ?
 *   findByUsername(username)     → SELECT * FROM users WHERE username = ?
 *   existsByEmail(email)         → SELECT COUNT(*) > 0 WHERE email = ?
 *   existsByUsername(username)   → SELECT COUNT(*) > 0 WHERE username = ?
 *
 *   Không cần viết @Query hay SQL! Spring tự parse tên method → query.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Tìm user theo email (dùng cho đăng nhập).
     * Optional<User>: Có thể trả về empty nếu không tìm thấy
     * → Tránh NullPointerException, phải xử lý trường hợp không có kết quả.
     */
    Optional<User> findByEmail(String email);

    /**
     * Tìm user theo username (dùng cho mention @username trong chat).
     */
    Optional<User> findByUsername(String username);

    /**
     * Kiểm tra email đã tồn tại chưa (dùng khi đăng ký).
     * boolean thay vì Optional → Gọn hơn khi chỉ cần biết có/không.
     */
    boolean existsByEmail(String email);

    /**
     * Kiểm tra username đã tồn tại chưa (dùng khi đăng ký).
     */
    boolean existsByUsername(String username);
}
