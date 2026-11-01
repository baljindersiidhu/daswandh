package com.daswandh.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateDonationRequest(
        @NotNull(message = "Amount is required")
        @Min(value = 100, message = "Minimum donation is INR 1 (100 paise)")
        @Max(value = 50_000_000, message = "Maximum donation is INR 5,00,000")
        Long amountPaise,

        boolean anonymous
) {}