package com.daswandh.backend.exception;

import org.springframework.http.HttpStatus;

// 401: "we don't know who you are"
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}