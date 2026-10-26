package com.daswandh.backend.entity;

import com.daswandh.backend.enums.MilestoneStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "milestones")
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    private int sequenceNo; // 1, 2, 3... defines the order of release
    private String title;
    private String description;
    private long amountPaise;

    @Enumerated(EnumType.STRING)
    private MilestoneStatus status = MilestoneStatus.LOCKED;

    protected Milestone() {}

    public Milestone(Campaign campaign, int sequenceNo, String title, String description, long amountPaise) {
        this.campaign = campaign;
        this.sequenceNo = sequenceNo;
        this.title = title;
        this.description = description;
        this.amountPaise = amountPaise;
    }

    public Long getId() { return id; }
    public Campaign getCampaign() { return campaign; }
    public int getSequenceNo() { return sequenceNo; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getAmountPaise() { return amountPaise; }
    public MilestoneStatus getStatus() { return status; }

    public void setStatus(MilestoneStatus status) { this.status = status; }
}