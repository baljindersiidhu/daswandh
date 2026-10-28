package com.daswandh.backend.dto;

import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.enums.CampaignStatus;

import java.time.LocalDate;

public record CampaignSummaryResponse(Long id, String title, long goalPaise, long raisedPaise,
                                      LocalDate deadline, CampaignStatus status, String creatorName) {

    public static CampaignSummaryResponse from(Campaign c, long raisedPaise) {
        return new CampaignSummaryResponse(c.getId(), c.getTitle(), c.getGoalPaise(), raisedPaise,
                c.getDeadline(), c.getStatus(), c.getCreator().getFullName());
    }
}