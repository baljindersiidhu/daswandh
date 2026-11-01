package com.daswandh.backend.dto;

import com.daswandh.backend.entity.Donation;
import com.daswandh.backend.enums.DonationStatus;

import java.time.Instant;

public record DonationResponse(Long id, Long campaignId, long amountPaise, DonationStatus status, Instant createdAt) {

    public static DonationResponse from(Donation d) {
        return new DonationResponse(d.getId(), d.getCampaign().getId(), d.getAmountPaise(),
                d.getStatus(), d.getCreatedAt());
    }
}