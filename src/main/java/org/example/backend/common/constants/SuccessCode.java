package org.example.backend.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessCode {
    //Register
    REGISTER_SUCCESS("Đăng ký thành công", HttpStatus.CREATED);

    private final String message;
    private final HttpStatus status;
}
