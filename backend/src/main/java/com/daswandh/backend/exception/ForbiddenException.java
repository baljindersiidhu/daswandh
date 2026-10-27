package com.daswandh.backend.exception;

import org.springframework.http.HttpStatus;

// 403: "we know who you are, and you're not allowed"
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}