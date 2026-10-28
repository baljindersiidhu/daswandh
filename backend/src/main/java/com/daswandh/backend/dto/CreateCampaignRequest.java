package com.daswandh.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record CreateCampaignRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 150, message = "Title must be 5 to 150 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 5000, message = "Description must be 10 to 5000 characters")
        String description,

        @NotNull(message = "Goal is required")
        @Min(value = 10_000, message = "Goal must be at least 10000 paise (INR 100)")
        @Max(value = 10_000_000_000L, message = "Goal is too large")
        Long goalPaise,

        @NotNull(message = "Deadline is required")
        @Future(message = "Deadline must be in the future")
        LocalDate deadline,

        // @Valid makes Spring validate each MilestoneRequest inside the list too.
        // Errors then appear as e.g. "milestones[0].title".
        @NotEmpty(message = "At least one milestone is required")
        @Size(max = 10, message = "At most 10 milestones are allowed")
        @Valid
        List<MilestoneRequest> milestones
) {}