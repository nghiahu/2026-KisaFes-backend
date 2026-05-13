package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.UpdateProfileRequest;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.dto.response.UserProfileResponse;
import org.example.backend.service.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/users")
public class UserController extends BaseController {

    private final IUserService userService;

    @GetMapping("/me")
    public ResponseEntity<ResponseWrapper<UserProfileResponse>> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return success(userService.getUserProfile(email), "Lấy thông tin cá nhân thành công");
    }

    @PutMapping("/me")
    public ResponseEntity<ResponseWrapper<UserProfileResponse>> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        String email = authentication.getName();
        return success(userService.updateProfile(email, request), "Cập nhật thông tin cá nhân thành công");
    }
}
