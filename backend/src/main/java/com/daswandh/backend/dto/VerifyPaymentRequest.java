package com.daswandh.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyPaymentRequest(
        @NotBlank(message = "Order id is required") String razorpayOrderId,
        @NotBlank(message = "Payment id is required") String razorpayPaymentId,
        @NotBlank(message = "Signature is required") String razorpaySignature
) {}