package com.daswandh.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectProofRequest(
        @NotBlank(message = "A reason is required when rejecting a proof")
        @Size(max = 1000, message = "Reason must be at most 1000 characters")
        String note
) {}