package com.daswandh.backend.exception;

import org.springframework.http.HttpStatus;

// 502: a service we depend on (Razorpay) failed. Not the client's fault, not a bug in our code.
public class ExternalServiceException extends ApiException {
    public ExternalServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}