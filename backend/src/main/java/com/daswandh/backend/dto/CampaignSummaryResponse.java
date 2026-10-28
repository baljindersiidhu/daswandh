package com.daswandh.backend.dto;

import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.enums.CampaignStatus;

import java.time.LocalDate;

public record CampaignSummaryResponse(Long id, String title, long goalPaise, LocalDate deadline,
                                      CampaignStatus status, String creatorName) {

    public static CampaignSummaryResponse from(Campaign c) {
        return new CampaignSummaryResponse(c.getId(), c.getTitle(), c.getGoalPaise(),
                c.getDeadline(), c.getStatus(), c.getCreator().getFullName());
    }
}