package org.example.backend.common.exception;

import lombok.Getter;
import org.example.backend.common.constants.ErrorCode;

@Getter
public class CustomBusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomBusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}
