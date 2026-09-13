package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard envelope for every REST response in this service. Use
 * {@link #success(Object)} / {@link #error(String, String)} rather than the
 * constructor directly so every endpoint stays consistent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    /** Machine-readable code for client-side branching (e.g. "REFERRAL_EXPIRED"). Null for generic errors. */
    private String errorCode;

    public static <T> MyApiResponse<T> success(T data) {
        return success(data, "Success");
    }

    public static <T> MyApiResponse<T> success(T data, String message) {
        return MyApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> MyApiResponse<T> error(String message, String errorCode) {
        return MyApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .errorCode(errorCode)
                .build();
    }
}
