package com.daswandh.backend.dto;

import com.daswandh.backend.entity.Milestone;
import com.daswandh.backend.enums.MilestoneStatus;

public record MilestoneResponse(Long id, int sequenceNo, String title, String description,
                                long amountPaise, MilestoneStatus status) {

    public static MilestoneResponse from(Milestone m) {
        return new MilestoneResponse(m.getId(), m.getSequenceNo(), m.getTitle(),
                m.getDescription(), m.getAmountPaise(), m.getStatus());
    }
}