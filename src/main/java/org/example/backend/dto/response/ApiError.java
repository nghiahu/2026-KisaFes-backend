package org.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiError {
    private LocalDateTime timestamp;
    private int status;
    private String error;      // BAD_REQUEST, NOT_FOUND...
    private String errorCode;  // BUSINESS CODE
    private String message;
    private String path;
    private Object details;    // validation errors
}