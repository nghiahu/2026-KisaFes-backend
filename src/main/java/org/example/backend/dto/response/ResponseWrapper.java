package org.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseWrapper<T> {
    private int status;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ResponseWrapper<T> of(int status, String message, T data) {
        ResponseWrapper<T> wrapper = new ResponseWrapper<>();
        wrapper.setStatus(status);
        wrapper.setMessage(message);
        wrapper.setData(data);
        wrapper.setTimestamp(LocalDateTime.now());
        return wrapper;
    }
}
