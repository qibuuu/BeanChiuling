package com.beanchiuling.security;

import com.beanchiuling.module.user.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter chặn TỪNG REQUEST để kiểm tra JWT token.
 *
 * 🎯 Filter hoạt động thế nào?
 *   Mỗi HTTP request đến server đều đi qua một "chain" các Filter,
 *   theo thứ tự từ trên xuống, trước khi đến Controller.
 *
 *   Request → [JwtAuthFilter] → [CorsFilter] → [SecurityFilter] → Controller
 *
 *   Filter này đọc header "Authorization: Bearer <token>",
 *   validate token, và set Authentication vào SecurityContext.
 *
 * 🎯 OncePerRequestFilter:
 *   Đảm bảo filter chỉ chạy ĐÚNG 1 LẦN mỗi request
 *   (tránh bị gọi 2 lần trong một số trường hợp forward/dispatch).
 *
 * 🎯 SecurityContextHolder:
 *   "Kho" lưu trữ thông tin user đang đăng nhập trong thread hiện tại.
 *   Controller có thể gọi SecurityContextHolder.getContext().getAuthentication()
 *   để lấy user đang login.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Bước 1: Đọc header Authorization
        final String authHeader = request.getHeader("Authorization");

        // Nếu không có header hoặc không bắt đầu bằng "Bearer " → bỏ qua, tiếp tục chain
        // (Request này sẽ bị Spring Security block nếu endpoint yêu cầu auth)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bước 2: Tách lấy token (bỏ "Bearer " prefix — 7 ký tự)
        final String jwt = authHeader.substring(7);

        try {
            // Bước 3: Extract email từ token
            final String email = jwtService.extractEmail(jwt);

            // Bước 4: Chỉ process nếu email có trong token VÀ chưa có authentication
            // (Tránh authenticate lại nếu request này đã được xử lý)
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Bước 5: Load user từ DB bằng email
                User user = (User) userDetailsService.loadUserByUsername(email);

                // Bước 6: Validate token — chữ ký đúng? Chưa hết hạn? Đúng user?
                if (jwtService.isTokenValid(jwt, user) && jwtService.isAccessToken(jwt)) {

                    // Bước 7: Tạo Authentication object và set vào SecurityContext
                    // → Từ đây, Spring Security biết request này là từ user đã xác thực
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,           // principal — user đang login
                                    null,           // credentials — null vì đã verify bằng JWT
                                    user.getAuthorities() // granted authorities (rỗng với hệ thống của ta)
                            );

                    // Gắn thêm thông tin request (IP, session ID...) vào authentication
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set vào SecurityContext của thread hiện tại
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Authenticated user: {} for URI: {}", email, request.getRequestURI());
                }
            }

        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            // Không set authentication → request sẽ bị treat như anonymous
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }

        // Bước 8: Dù có authenticate hay không, vẫn tiếp tục chain
        filterChain.doFilter(request, response);
    }
}
