package org.example.backend.service.impl;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.AppConstants;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.constants.SuccessCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.LoginRequest;
import org.example.backend.dto.request.RegisterRequest;
import org.example.backend.dto.response.LoginResponse;
import org.example.backend.dto.response.RegisterResponse;
import org.example.backend.dto.response.UserInfoResponse;
import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.repository.IRoleRepository;
import org.example.backend.repository.IUserRepository;
import org.example.backend.security.jwt.JwtProvider;
import org.example.backend.service.IAuthService;
import org.example.backend.service.RedisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository
                .findByEmailOrUserName(
                        loginRequest.getUsername(),
                        loginRequest.getUsername()
                )
                .orElseThrow(() ->
                        new CustomBusinessException(
                                ErrorCode.USER_NOT_FOUND
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
                "refestToken",
                refreshToken,604800000
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900000)
                .user(
                UserInfoResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .username(user.getUserName())
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


}
