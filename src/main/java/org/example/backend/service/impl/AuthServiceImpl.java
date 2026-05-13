package org.example.backend.service.impl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.AppConstants;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.constants.SuccessCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.LoginRequest;
import org.example.backend.dto.request.RegisterRequest;
import org.example.backend.dto.request.ResetPasswordRequest;
import org.example.backend.dto.response.*;
import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.repository.IRoleRepository;
import org.example.backend.repository.IUserRepository;
import org.example.backend.security.jwt.JwtProvider;
import org.example.backend.security.principle.MyUserDetailsService;
import org.example.backend.service.IAuthService;
import org.example.backend.service.RedisService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final IUserRepository  userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final IRoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;
    private final MyUserDetailsService myUserDetailsService;

    @Override
    public Boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Boolean checkUsername(String username) {
        return userRepository.existsUserByUserName(username);
    }


    @Override
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomBusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if(userRepository.existsUserByUserName(request.getUsername())) {
            throw new CustomBusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        if (!jwtProvider.isOtpVerificationToken(request.getVerifyToken())) {
            throw new CustomBusinessException(ErrorCode.INVALID_VERIFY_TOKEN);
        }

        String verifiedEmail = jwtProvider.extractUsername(request.getVerifyToken());

        if (!verifiedEmail.equals(request.getEmail())) {
            throw new CustomBusinessException(ErrorCode.INVALID_VERIFY_TOKEN);
        }

        Role userRole = roleRepository.findByName(AppConstants.ROLE_USER)
                .orElseThrow(() ->
                        new CustomBusinessException(ErrorCode.ROLE_NOT_FOUND)
                );

        User user = User.builder()
                .fullName(request.getFullName())
                .userName(request.getUsername())
                .bio(request.getBio())
                .avatar(request.getAvatar())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);

        return RegisterResponse.builder()
                .message(SuccessCode.REGISTER_SUCCESS.getMessage())
                .email(user.getEmail())
                .createdAt(Instant.now())
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest, HttpServletResponse response) {
        User user = userRepository
                .findByEmailOrUserName(
                        loginRequest.getUsername(),
                        loginRequest.getUsername()
                )
                .orElseThrow(() ->
                        new CustomBusinessException(
                                ErrorCode.INVALID_CREDENTIALS
                        ));
        // xác thực password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        loginRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        // sinh token
        String accessToken = jwtProvider.generateAccessToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(authentication);

        redisService.save(
                "refreshToken:" + user.getEmail(),
                refreshToken, 604800000
        );
        addRefreshTokenCookie(response, refreshToken);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(900000)
                .user(
                UserInfoResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .username(user.getUserName())
                        .fullname(user.getFullName())
                        .avatarUrl(user.getAvatar())
                        .roles(
                            user.getRoles()
                                    .stream()
                                    .map(Role::getName)
                                    .collect(Collectors.toSet())
                    )
                    .build()
                )
                .build();
    }

    @Override
    public JwtResponse handleOAuth2Callback(String token, HttpServletResponse response) {
        if (token == null || token.isBlank()) {
            throw new CustomBusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            Date expiration = jwtProvider.extractExpiration(token);
            if (expiration.before(new Date())) {
                throw new CustomBusinessException(ErrorCode.UNAUTHORIZED);
            }
        } catch (CustomBusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomBusinessException(ErrorCode.UNAUTHORIZED);
        }

        String email = jwtProvider.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND));

        UserDetails userDetails = myUserDetailsService.loadUserByUsername(user.getEmail());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String accessToken = jwtProvider.generateAccessToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(authentication);

        UserInfoResponse userInfo = UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUserName())
                .fullname(user.getFullName())
                .avatarUrl(user.getAvatar())
                .roles(
                        user.getRoles()
                                .stream()
                                .map(Role::getName)
                                .collect(Collectors.toSet())
                )
                .build();

        redisService.save("refreshToken:" + user.getEmail(), refreshToken, 604800000);
        addRefreshTokenCookie(response, refreshToken);

        return JwtResponse.builder()
                .accessToken(accessToken)
                .expiresIn(900000)
                .user(userInfo)
                .build();
    }

    @Override
    public TokenRefreshResponse refreshToken(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_REFRESH_TOKEN
            );
        }

        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_REFRESH_TOKEN
            );
        }

        String identifier = jwtProvider.extractUsername(refreshToken);
        User user = userRepository.findByUserNameOrEmail(identifier, identifier)
                .orElseThrow(() ->
                        new CustomBusinessException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );

        if (!user.isActive()) {
            throw new CustomBusinessException(
                    ErrorCode.ACCOUNT_DISABLED
            );
        }

        // Kiểm tra token có khớp với token đang lưu trong Redis không (để xử lý vụ logout)
        Object cachedToken = redisService.get("refreshToken:" + user.getEmail());
        if (cachedToken == null || !cachedToken.toString().equals(refreshToken)) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_REFRESH_TOKEN
            );
        }

        /*
         * Create authentication object
         */
        UserDetails userDetails =
                myUserDetailsService.loadUserByUsername(
                        user.getEmail()
                );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        /*
         * Generate new tokens
         */
        String newAccessToken =
                jwtProvider.generateAccessToken(authentication);

        String newRefreshToken =
                jwtProvider.generateRefreshToken(authentication);

        /*
         * Rotate refresh token — save to Redis
         */
        redisService.save(
                "refreshToken:" + user.getEmail(),
                newRefreshToken, 604800000
        );
        addRefreshTokenCookie(
                response,
                newRefreshToken
        );

        /*
         * Return access token only
         */
        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .expiresIn(900000)
                .build();
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!jwtProvider.isResetPasswordVerificationToken(request.getToken())) {
            throw new CustomBusinessException(ErrorCode.INVALID_VERIFY_TOKEN);
        }

        String verifiedEmail = jwtProvider.extractUsername(request.getToken());
        if (!verifiedEmail.equals(request.getEmail())) {
            throw new CustomBusinessException(ErrorCode.INVALID_VERIFY_TOKEN);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private void addRefreshTokenCookie(
            HttpServletResponse response,
            String refreshToken
    ) {

        ResponseCookie cookie =
                ResponseCookie.from(
                                "refreshToken",
                                refreshToken
                        )
                        .httpOnly(true)
                        .secure(false) // true when HTTPS
                        .path("/")
                        .sameSite("Lax")
                        .maxAge(7 * 24 * 60 * 60)
                        .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    @Override
    public void logout(String refreshToken, HttpServletResponse response) {
        // Xóa refresh token trong Redis
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                String email = jwtProvider.extractUsername(refreshToken);
                redisService.delete("refreshToken:" + email);
            } catch (Exception ignored) {
                // Token không hợp lệ thì bỏ qua
            }
        }

        // Xóa cookie refreshToken
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}


