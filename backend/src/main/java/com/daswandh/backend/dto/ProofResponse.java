package com.daswandh.backend.dto;

import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.entity.Milestone;
import com.daswandh.backend.entity.Proof;
import com.daswandh.backend.enums.ProofStatus;

import java.time.Instant;

public record ProofResponse(Long id, Long milestoneId, String milestoneTitle, Long campaignId,
                            String campaignTitle, String fileUrl, String description,
                            long amountSpentPaise, ProofStatus status, String reviewNote, Instant createdAt) {

    public static ProofResponse from(Proof p) {
        Milestone m = p.getMilestone();
        Campaign c = m.getCampaign();
        return new ProofResponse(p.getId(), m.getId(), m.getTitle(), c.getId(), c.getTitle(),
                p.getFileUrl(), p.getDescription(), p.getAmountSpentPaise(), p.getStatus(),
                p.getReviewNote(), p.getCreatedAt());
    }
}