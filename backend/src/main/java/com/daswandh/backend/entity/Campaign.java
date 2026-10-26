package com.daswandh.backend.entity;

import com.daswandh.backend.enums.CampaignStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: the creator row is only loaded if we actually call getCreator().getXxx()
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private User creator;

    private String title;
    private String description;
    private long goalPaise; // money is always stored in paise
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(updatable = false)
    private Instant createdAt;

    protected Campaign() {}

    public Campaign(User creator, String title, String description, long goalPaise, LocalDate deadline) {
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.goalPaise = goalPaise;
        this.deadline = deadline;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public Long getId() { return id; }
    public User getCreator() { return creator; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getGoalPaise() { return goalPaise; }
    public LocalDate getDeadline() { return deadline; }
    public CampaignStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(CampaignStatus status) { this.status = status; }
}