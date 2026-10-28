package com.daswandh.backend.dto;

public record LedgerResponse(LedgerSummary summary, PageResponse<LedgerEntryResponse> entries) {}