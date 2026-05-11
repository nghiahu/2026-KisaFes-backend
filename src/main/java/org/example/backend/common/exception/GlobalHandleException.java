package org.example.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.dto.response.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalHandleException {

    @ExceptionHandler(CustomBusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(CustomBusinessException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();

        log.warn("Business error: {}", errorCode.name());

        return buildResponse(errorCode, request, null);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getFieldErrors().forEach(err ->
                errors.put(err.getField(), err.getDefaultMessage())
        );

        return buildResponse(ErrorCode.VALIDATION_ERROR, request, errors);
    }

    // ACCESS DENIED
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(HttpServletRequest request) {
        return buildResponse(ErrorCode.FORBIDDEN, request, null);
    }

    // NOT FOUND (endpoint)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(HttpServletRequest request) {
        return buildResponse(ErrorCode.USER_NOT_FOUND, request, null);
    }

    // FALLBACK (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception", ex);

        return buildResponse(ErrorCode.INTERNAL_ERROR, request, null);
    }

    // COMMON BUILDER
    private ResponseEntity<ApiError> buildResponse(ErrorCode errorCode,
                                                   HttpServletRequest request,
                                                   Object details) {

        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getStatus().name())
                .errorCode(errorCode.name())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .details(details)
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(error);
    }
}
