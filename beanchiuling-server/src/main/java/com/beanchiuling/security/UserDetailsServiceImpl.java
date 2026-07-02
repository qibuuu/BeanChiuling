package com.beanchiuling.security;

import com.beanchiuling.module.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Cầu nối giữa Spring Security và Database của chúng ta.
 *
 * 🎯 Spring Security cần gì?
 *   Khi authenticate, Spring Security gọi: userDetailsService.loadUserByUsername(email)
 *   → Cần tìm user trong DB bằng email
 *   → So sánh password (đã hash) với password user nhập vào
 *
 * 🎯 Tại sao không inject trực tiếp UserRepository vào SecurityConfig?
 *   Để tránh circular dependency (vòng phụ thuộc) giữa các bean.
 *   Tách thành service riêng → clean hơn.
 *
 * @RequiredArgsConstructor (Lombok): Tự tạo constructor inject tất cả final fields.
 *   Tương đương:
 *     public UserDetailsServiceImpl(UserRepository userRepository) {
 *         this.userRepository = userRepository;
 *     }
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Spring Security gọi method này khi cần authenticate user.
     * "username" ở đây thực ra là EMAIL (vì ta override getUsername() trong User entity).
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));
    }
}
