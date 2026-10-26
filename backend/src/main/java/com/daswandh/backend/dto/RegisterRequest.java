package com.daswandh.backend.dto;

import com.daswandh.backend.enums.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        // BCrypt only uses the first 72 bytes of a password, so we cap the length
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role
) {}