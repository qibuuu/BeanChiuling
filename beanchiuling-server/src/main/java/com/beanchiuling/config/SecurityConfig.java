package com.beanchiuling.config;

import com.beanchiuling.security.JwtAuthenticationFilter;
import com.beanchiuling.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cấu hình toàn bộ Spring Security.
 *
 * 🎯 @Configuration: Class này là nguồn định nghĩa các @Bean.
 * 🎯 @EnableWebSecurity: Bật Spring Security cho ứng dụng web.
 *
 * 🎯 @Bean là gì?
 *   Các method có @Bean sẽ được Spring quản lý vòng đời.
 *   Khi class khác cần (inject) thì Spring tự cung cấp instance.
 *   VD: AuthService cần PasswordEncoder → Spring inject bean passwordEncoder() này.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * SecurityFilterChain: Định nghĩa các quy tắc bảo mật HTTP.
     *
     * Đây là method QUAN TRỌNG NHẤT trong SecurityConfig.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ① Tắt CSRF protection
                // 🎯 TẠI SAO tắt CSRF?
                //   CSRF cần cookie/session để hoạt động.
                //   Chúng ta dùng JWT stateless → không có session → CSRF không cần thiết.
                .csrf(AbstractHttpConfigurer::disable)

                // ② Cấu hình CORS (Cross-Origin Resource Sharing)
                // Cho phép Frontend (localhost:3000) gọi API từ Backend (localhost:8080)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ③ Quy tắc phân quyền cho từng endpoint
                .authorizeHttpRequests(auth -> auth
                        // Các endpoint PUBLIC — không cần đăng nhập
                        .requestMatchers(
                                "/api/v1/auth/**",          // Đăng ký, đăng nhập, refresh
                                "/actuator/health",          // Health check
                                "/ws/**"                     // WebSocket handshake
                        ).permitAll()

                        // Tất cả endpoint còn lại → yêu cầu phải có JWT hợp lệ
                        .anyRequest().authenticated()
                )

                // ④ Stateless Session — KHÔNG tạo HTTP Session
                // 🎯 TẠI SAO STATELESS?
                //   Traditional: Server lưu session trong memory/Redis
                //   JWT: Server không lưu gì → scale dễ hơn (nhiều server instance)
                //   Mỗi request mang JWT → server tự verify → không cần lookup
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ⑤ Gắn AuthenticationProvider — dạy Spring Security cách authenticate
                .authenticationProvider(authenticationProvider())

                // ⑥ Chèn JwtAuthFilter TRƯỚC UsernamePasswordAuthenticationFilter
                // 🎯 THỨ TỰ QUAN TRỌNG:
                //   [JwtAuthFilter] → [UsernamePasswordAuthFilter] → [SecurityFilter]
                //   JwtFilter phải chạy trước để set authentication vào context
                //   trước khi Spring Security kiểm tra authorization
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationProvider: Dạy Spring Security cách xác thực.
     *
     * DaoAuthenticationProvider (Dao = Data Access Object):
     *   - Dùng UserDetailsService để load user từ DB
     *   - Dùng PasswordEncoder để so sánh password hash
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * AuthenticationManager: Interface để thực hiện authenticate.
     * AuthService sẽ dùng manager này để authenticate email/password.
     *
     * AuthenticationConfiguration tự inject bởi Spring → lấy manager từ đó.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * PasswordEncoder: Dùng BCrypt để hash password.
     *
     * 🎯 BCrypt là gì?
     *   - Thuật toán hash một chiều (không giải mã được)
     *   - Tự động thêm "salt" ngẫu nhiên → cùng password nhưng hash khác nhau mỗi lần
     *   - "123456" → "$2a$10$N9qo8uLOickgx..." (60 ký tự)
     *   - strength = 10 (mặc định): 2^10 = 1024 vòng lặp → chậm vừa đủ để chống brute force
     *
     *   KHÔNG BAO GIỜ lưu plain text password!
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS Configuration: Cho phép Frontend gọi API cross-origin.
     *
     * 🎯 CORS là gì?
     *   Browser block request từ origin A đến origin B nếu server B không cho phép.
     *   Frontend: http://localhost:3000 (origin A)
     *   Backend:  http://localhost:8080 (origin B)
     *   → Cần cấu hình CORS để browser cho phép.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Cho phép request từ Frontend dev server
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",   // Next.js dev
                "http://localhost:3001"    // Dự phòng
        ));

        // Cho phép các HTTP methods thông dụng
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Cho phép tất cả headers (bao gồm Authorization)
        configuration.setAllowedHeaders(List.of("*"));

        // Cho phép gửi credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);

        // Cache CORS preflight response trong 1 giờ
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
