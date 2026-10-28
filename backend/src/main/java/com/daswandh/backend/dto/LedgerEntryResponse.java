package com.daswandh.backend.dto;

import com.daswandh.backend.entity.LedgerEntry;
import com.daswandh.backend.enums.LedgerEntryType;

import java.time.Instant;

// Deliberately contains no donor name: donors keep their privacy on the public ledger.
public record LedgerEntryResponse(Long id, LedgerEntryType type, long amountPaise, String description,
                                  Long donationId, Long milestoneId, Instant createdAt) {

    public static LedgerEntryResponse from(LedgerEntry e) {
        return new LedgerEntryResponse(e.getId(), e.getEntryType(), e.getAmountPaise(), e.getDescription(),
                e.getDonation() == null ? null : e.getDonation().getId(),
                e.getMilestone() == null ? null : e.getMilestone().getId(),
                e.getCreatedAt());
    }
}