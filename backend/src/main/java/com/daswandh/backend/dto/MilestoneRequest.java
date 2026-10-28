package com.daswandh.backend.dto;

import jakarta.validation.constraints.*;

public record MilestoneRequest(
        @NotBlank(message = "Milestone title is required")
        @Size(max = 150, message = "Milestone title must be at most 150 characters")
        String title,

        @Size(max = 2000, message = "Milestone description must be at most 2000 characters")
        String description,

        // The upper limit also guarantees our sums can never overflow a long.
        @NotNull(message = "Milestone amount is required")
        @Positive(message = "Milestone amount must be greater than 0")
        @Max(value = 10_000_000_000L, message = "Milestone amount is too large")
        Long amountPaise
) {}