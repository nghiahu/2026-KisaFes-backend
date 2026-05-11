package org.example.backend.service;
import org.example.backend.dto.request.LoginRequest;
import org.example.backend.dto.request.RegisterRequest;
import org.example.backend.dto.response.LoginResponse;
import org.example.backend.dto.response.RegisterResponse;

public interface IAuthService {
    Boolean checkEmail(String email);
    Boolean checkUsername(String username);
    RegisterResponse register(RegisterRequest registerRequest);
    LoginResponse login(LoginRequest loginRequest);
}
