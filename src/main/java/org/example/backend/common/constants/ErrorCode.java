package org.example.backend.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // USER
    USER_NOT_FOUND("User không tồn tại", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("User đã tồn tại", HttpStatus.CONFLICT),
    RESOURCE_NOT_FOUND("Không tìm thấy dữ liệu", HttpStatus.NOT_FOUND),

    // AUTH
    INVALID_CREDENTIALS("Sai tài khoản hoặc mật khẩu", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("Phiên đăng nhập hết hạn hoặc không hợp lệ", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED("Tài khoản đã bị khóa", HttpStatus.FORBIDDEN),

    // VALIDATION
    VALIDATION_ERROR("Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),

    // OTP
    INVALID_OTP("Mã OTP không chính xác vui lòng kiểm tra lại", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED("Mã OTP đã hết hạn vui lòng thử lại", HttpStatus.CONFLICT),
    OTP_ALREADY_VERIFIED("OTP đã được xác thực", HttpStatus.CONFLICT),
    INVALID_VERIFY_TOKEN("Token xác thực không hợp lệ", HttpStatus.UNAUTHORIZED),
    VERIFY_TOKEN_EXPIRED("Token xác thực đã hết hạn", HttpStatus.UNAUTHORIZED),
    EMAIL_NOT_VERIFIED("Email chưa được xác thực OTP", HttpStatus.FORBIDDEN),

    // CATEGORY
    RESOURCE_ALREADY_EXISTS("Tài nguyên không có sẵn",  HttpStatus.BAD_REQUEST),

    // REGISTER
    EMAIL_ALREADY_EXISTS("Email đã tồn tại", HttpStatus.CONFLICT),
    ROLE_NOT_FOUND("Không xác định được phân quyền của hệ thống", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS("Tên người dùng đã tôn tại", HttpStatus.CONFLICT),

    // SYSTEM
    INVALID_REFRESH_TOKEN("Refresh Token không hợp lệ", HttpStatus.UNAUTHORIZED),
    INTERNAL_ERROR("Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR);



    private final String message;
    private final HttpStatus status;
}
