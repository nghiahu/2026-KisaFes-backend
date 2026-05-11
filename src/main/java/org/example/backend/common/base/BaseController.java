package org.example.backend.common.base;

import org.example.backend.dto.response.ResponseWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public abstract class BaseController {

    protected <T> ResponseEntity<ResponseWrapper<T>> success(T data, String message) {
        return buildResponse(HttpStatus.OK, message, data);
    }

    protected <T> ResponseEntity<ResponseWrapper<T>> created(T data, String message) {
        return buildResponse(HttpStatus.CREATED, message, data);
    }

    protected ResponseEntity<ResponseWrapper<Object>> error(HttpStatus status, String message) {
        return buildResponse(status, message, null);
    }

    private <T> ResponseEntity<ResponseWrapper<T>> buildResponse(HttpStatus status, String message, T data) {
        ResponseWrapper<T> response = ResponseWrapper.of(status.value(), message, data);
        return ResponseEntity.status(status).body(response);
    }
}
