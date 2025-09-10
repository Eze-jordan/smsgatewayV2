package com.ogooueTech.smsgateway.exception;


import java.time.LocalDateTime;

public record ApiError(String error, String message, int status, String path, LocalDateTime timestamp) {
    public static ApiError of(String error, String message, int status, String path) {
        return new ApiError(error, message, status, path, LocalDateTime.now());
    }
}