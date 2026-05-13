package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank(message = "Email không được để trống")
    @Pattern(
            regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$",
            message = "Email không đúng định dạng"
    )
    private String email;
    @NotBlank(message = "Mật khẩu mới không được để trống")
    private String newPassword;
    @NotBlank(message = "Mã xác thực không được để trống")
    private String token;
}
