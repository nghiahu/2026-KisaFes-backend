package org.example.backend.service;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.dto.request.LoginRequest;
import org.example.backend.dto.request.RegisterRequest;
import org.example.backend.dto.request.ResetPasswordRequest;
import org.example.backend.dto.response.JwtResponse;
import org.example.backend.dto.response.LoginResponse;
import org.example.backend.dto.response.RegisterResponse;
import org.example.backend.dto.response.TokenRefreshResponse;

public interface IAuthService {
    Boolean checkEmail(String email);
    Boolean checkUsername(String username);
    RegisterResponse register(RegisterRequest registerRequest);
    LoginResponse login(LoginRequest loginRequest, HttpServletResponse response);
    JwtResponse handleOAuth2Callback(String token, HttpServletResponse response);
    TokenRefreshResponse refreshToken(String refreshToken, HttpServletResponse response);
    void resetPassword(ResetPasswordRequest request);
    void logout(String refreshToken, HttpServletResponse response);
}
