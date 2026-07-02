package com.beanchiuling.module.auth;

import com.beanchiuling.common.exception.AppException;
import com.beanchiuling.common.exception.ErrorCode;
import com.beanchiuling.module.auth.dto.LoginRequest;
import com.beanchiuling.module.auth.dto.RegisterRequest;
import com.beanchiuling.module.auth.dto.TokenResponse;
import com.beanchiuling.module.user.UserRepository;
import com.beanchiuling.module.user.entity.User;
import com.beanchiuling.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service xử lý toàn bộ business logic của Authentication.
 *
 * 🎯 @Service: Đánh dấu đây là tầng Business Logic.
 *   Controller → Service → Repository là luồng chuẩn trong Spring Boot.
 *
 * 🎯 @Transactional: Wrap method trong một database transaction.
 *   Nếu method throw exception → ROLLBACK tất cả thay đổi DB trong method đó.
 *   VD: Nếu save user thành công nhưng sau đó throw exception
 *       → User record sẽ bị rollback, không bị lưu lửng trong DB.
 *
 * 🎯 @Slf4j: Tạo biến `log` để ghi log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Đăng ký tài khoản mới.
     *
     * Flow:
     * 1. Validate email chưa tồn tại
     * 2. Validate username chưa tồn tại
     * 3. Hash password bằng BCrypt
     * 4. Lưu user vào DB
     * 5. Generate JWT tokens
     * 6. Trả về tokens + user info
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        // Bước 1: Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Bước 2: Kiểm tra username đã tồn tại chưa
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // Bước 3: Tạo User entity với password đã hash
        // Builder pattern: User.builder()...build() thay vì new User() + setters
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                // passwordEncoder.encode() → BCrypt hash password
                .password(passwordEncoder.encode(request.getPassword()))
                // Nếu không nhập displayName → dùng username làm mặc định
                .displayName(request.getDisplayName() != null
                        ? request.getDisplayName()
                        : request.getUsername())
                .build();

        // Bước 4: Lưu vào database (Hibernate tự INSERT)
        User savedUser = userRepository.save(user);
        log.info("New user registered: {} ({})", savedUser.getUsername(), savedUser.getId());

        // Bước 5 + 6: Generate tokens và trả về response
        return buildTokenResponse(savedUser);
    }

    /**
     * Đăng nhập với email + password.
     *
     * Flow:
     * 1. Dùng AuthenticationManager để authenticate
     *    → Tự động: load user bằng email, so sánh BCrypt hash
     * 2. Nếu đúng → generate tokens
     * 3. Nếu sai → throw exception
     */
    public TokenResponse login(LoginRequest request) {
        try {
            // AuthenticationManager.authenticate() sẽ:
            //   1. Gọi UserDetailsService.loadUserByUsername(email) → load user từ DB
            //   2. So sánh password: passwordEncoder.matches(rawPassword, storedHash)
            //   3. Nếu đúng → trả về Authentication object
            //   4. Nếu sai → throw BadCredentialsException
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),    // principal
                            request.getPassword()  // credentials
                    )
            );
        } catch (BadCredentialsException e) {
            // Không lộ thông tin: "Email không tồn tại" hay "Sai password"
            // Luôn trả về cùng một thông báo để tránh User Enumeration Attack
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Nếu authenticate thành công → load user và generate tokens
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        log.info("User logged in: {} ({})", user.getUsername(), user.getId());
        return buildTokenResponse(user);
    }

    /**
     * Dùng Refresh Token để lấy Access Token mới.
     * (Khi Access Token hết hạn sau 24h)
     */
    public TokenResponse refreshToken(String refreshToken) {
        // Validate refresh token không hết hạn
        if (jwtService.isTokenExpired(refreshToken)) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        // Đảm bảo đây là refresh token (không phải access token)
        if (jwtService.isAccessToken(refreshToken)) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Chỉ generate Access Token mới, giữ nguyên Refresh Token cũ
        String newAccessToken = jwtService.generateAccessToken(user);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)   // Giữ nguyên
                .accessTokenExpiresAt(jwtService.extractExpiration(newAccessToken).getTime())
                .user(buildUserInfo(user))
                .build();
    }

    // ── Private Helpers ──────────────────────────────

    /**
     * Build TokenResponse từ User entity.
     * Dùng chung cho cả register và login.
     */
    private TokenResponse buildTokenResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresAt(jwtService.extractExpiration(accessToken).getTime())
                .user(buildUserInfo(user))
                .build();
    }

    private TokenResponse.UserInfo buildUserInfo(User user) {
        return TokenResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsernameValue())  // Dùng getUsernameValue() vì getUsername() trả về email
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
