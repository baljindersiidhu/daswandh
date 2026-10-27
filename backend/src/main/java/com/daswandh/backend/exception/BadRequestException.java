package com.daswandh.backend.exception;

import org.springframework.http.HttpStatus;

// 400: input that passes annotations but breaks a business rule (e.g. milestones don't add up to the goal)
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}