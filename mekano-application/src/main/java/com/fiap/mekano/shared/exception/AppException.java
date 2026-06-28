package com.fiap.mekano.shared.exception;

public class AppException extends RuntimeException {

    private final int statusCode;

    public AppException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
