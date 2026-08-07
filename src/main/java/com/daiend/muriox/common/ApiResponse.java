package com.daiend.muriox.common;

import java.util.List;

public record ApiResponse<T>(
        int code,
        String message,
        T data,
        List<FieldViolation> errors) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data, List.of());
    }

    public static  ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(200, message, null, List.of());
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(400, message, null, List.of());
    }

    public static ApiResponse<Void> fail(String message, List<FieldViolation> errors) {
        return new ApiResponse<>(400, message, null, errors);
    }

    public static ApiResponse<Void> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, List.of());
    }

}
