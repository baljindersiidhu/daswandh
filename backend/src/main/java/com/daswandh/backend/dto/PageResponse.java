package com.daswandh.backend.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// Our own stable JSON shape for paged results. We avoid returning Spring's Page directly,
// because its JSON structure is not meant to be a public API contract.
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}