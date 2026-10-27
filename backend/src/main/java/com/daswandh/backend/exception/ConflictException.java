package com.daswandh.backend.exception;

import org.springframework.http.HttpStatus;

// 409: the request is valid but clashes with current state (duplicate email, wrong milestone state...)
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}