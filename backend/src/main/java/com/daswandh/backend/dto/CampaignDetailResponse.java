package com.daswandh.backend.dto;

import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.entity.Milestone;
import com.daswandh.backend.enums.CampaignStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CampaignDetailResponse(Long id, String title, String description, long goalPaise,
                                     LocalDate deadline, CampaignStatus status,
                                     Long creatorId, String creatorName, Instant createdAt,
                                     List<MilestoneResponse> milestones) {

    public static CampaignDetailResponse from(Campaign c, List<Milestone> milestones) {
        return new CampaignDetailResponse(c.getId(), c.getTitle(), c.getDescription(), c.getGoalPaise(),
                c.getDeadline(), c.getStatus(), c.getCreator().getId(), c.getCreator().getFullName(),
                c.getCreatedAt(), milestones.stream().map(MilestoneResponse::from).toList());
    }
}