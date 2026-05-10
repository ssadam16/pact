package com.technokratos.pact.common.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private Map<String, String> errors;

    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
        this.errors = null;
    }

    public ApiResponse(boolean success, Map<String, String> errors) {
        this.success = success;
        this.errors = errors;
        this.data = null;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data);
    }

    public static <T> ApiResponse<T> error(Map<String, String> errors) {
        return new ApiResponse<>(false, errors);
    }
}
