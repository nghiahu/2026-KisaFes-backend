package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.LoginRequest;
import org.example.backend.dto.request.RegisterRequest;
import org.example.backend.dto.request.SendOtpRequest;
import org.example.backend.dto.request.VerifyOtpRequest;
import org.example.backend.dto.response.LoginResponse;
import org.example.backend.dto.response.RegisterResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.service.OtpService;
import org.example.backend.service.impl.AuthServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/register")
    public ResponseEntity<ResponseWrapper<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return created(authServiceImpl.register(request),
                "Đăng ký tài khoản thành công"
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseWrapper<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return success(authServiceImpl.login(request),
                "Đăng nhập thành công"
                );
    }
}
