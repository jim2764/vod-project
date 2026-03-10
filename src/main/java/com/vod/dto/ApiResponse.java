package com.vod.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp
) {
    private static <T> ApiResponse<T> create(boolean success, String code, String message, T data) {
        return new ApiResponse<>(success, code, message, data, LocalDateTime.now());
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return create(true, "200", "Success", data);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return create(false, "400", message, null);
    }

    public static <T> ApiResponse<T> internalServerError(String message) {
        return create(false, "500", message, null);
    }
}
