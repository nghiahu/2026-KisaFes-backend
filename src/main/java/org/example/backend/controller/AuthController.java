package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.*;
import org.example.backend.dto.response.*;
import org.example.backend.service.OtpService;
import org.example.backend.service.impl.AuthServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/auth")
public class AuthController extends BaseController {

    private final AuthServiceImpl authServiceImpl;
    private final OtpService otpServiceImpl;

    @GetMapping("/checkEmail")
    public ResponseEntity<ResponseWrapper<Boolean>> checkEmail(@RequestParam("email") String email) {
        return success(
                authServiceImpl.checkEmail(email),
                "Kiểm tra email"
        );
    }

    @GetMapping("/checkUsername")
    public ResponseEntity<ResponseWrapper<Boolean>> checkUsername(@RequestParam("username") String username) {
        return success(
                authServiceImpl.checkUsername(username),
                "Kiểm tra username"
        );
    }


    @PostMapping("/send-otp")
    public ResponseEntity<ResponseWrapper<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        otpServiceImpl.sendMail(request.getEmail());
        return success(null,
                "Gửi OTP thành công");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ResponseWrapper<String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return success(otpServiceImpl.verifyOtp(request.getEmail(), request.getOtp()),
                "Xác thực OTP thành công"
        );
    }

    @PostMapping("send-reset-password")
    public ResponseEntity<ResponseWrapper<Void>> resetPass(@Valid @RequestBody SendOtpRequest request) {
        if (!authServiceImpl.checkEmail(request.getEmail())) {
            throw new org.example.backend.common.exception.CustomBusinessException(org.example.backend.common.constants.ErrorCode.USER_NOT_FOUND);
        }
        otpServiceImpl.sendResetPasswordMail(request.getEmail());
        return success(null, "Gửi OTP đổi mật khẩu thành công");
    }

    @PostMapping("verify-otp-reset")
    public ResponseEntity<ResponseWrapper<String>> verifyOtpResetPass(@Valid @RequestBody VerifyOtpRequest request) {
        return success(otpServiceImpl.verifyResetPassword(request.getEmail(), request.getOtp()),
                "Xác thực OTP Đổi mật khẩu thành công");
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseWrapper<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return created(authServiceImpl.register(request),
                "Đăng ký tài khoản thành công"
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseWrapper<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return success(authServiceImpl.login(request, response),
                "Đăng nhập thành công"
                );
    }

    @PostMapping("/oauth2/callback")
    public ResponseEntity<ResponseWrapper<JwtResponse>>oauth2Callback(HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;
        return success(authServiceImpl.handleOAuth2Callback(token, response),
                "OAuth2 đăng nhập thành công");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponseWrapper<TokenRefreshResponse>> refreshToken
            (@CookieValue(name = "refreshToken", required = false)
            String refreshToken,
            HttpServletResponse response) {
        return success(
                authServiceImpl.refreshToken(refreshToken, response),
                "Refresh token thành công"
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResponseWrapper<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        authServiceImpl.resetPassword(request);
        return success(null,
                "Đổi mật khẩu thành công"
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseWrapper<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        authServiceImpl.logout(refreshToken, response);
        return success(null, "Đăng xuất thành công");
    }
}
