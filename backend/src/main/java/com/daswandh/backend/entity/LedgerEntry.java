package com.daswandh.backend.entity;

import com.daswandh.backend.enums.LedgerEntryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries")
@Immutable // Hibernate will never issue an UPDATE for this entity
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    private LedgerEntryType entryType;

    private long amountPaise; // always positive; entryType gives the direction

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id")
    private Donation donation; // set only for DONATION_IN

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id")
    private Milestone milestone; // set only for MILESTONE_RELEASE_OUT

    private String description;

    @Column(updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {}

    private LedgerEntry(Campaign campaign, LedgerEntryType entryType, long amountPaise,
                        Donation donation, Milestone milestone, String description) {
        this.campaign = campaign;
        this.entryType = entryType;
        this.amountPaise = amountPaise;
        this.donation = donation;
        this.milestone = milestone;
        this.description = description;
    }

    // Static factory methods make it impossible to build an invalid combination.
    public static LedgerEntry donationIn(Donation donation) {
        return new LedgerEntry(donation.getCampaign(), LedgerEntryType.DONATION_IN,
                donation.getAmountPaise(), donation, null, "Donation received");
    }

    public static LedgerEntry milestoneRelease(Milestone milestone) {
        return new LedgerEntry(milestone.getCampaign(), LedgerEntryType.MILESTONE_RELEASE_OUT,
                milestone.getAmountPaise(), null, milestone,
                "Released for milestone: " + milestone.getTitle());
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Campaign getCampaign() { return campaign; }
    public LedgerEntryType getEntryType() { return entryType; }
    public long getAmountPaise() { return amountPaise; }
    public Donation getDonation() { return donation; }
    public Milestone getMilestone() { return milestone; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}