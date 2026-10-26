package com.daswandh.backend.entity;

import com.daswandh.backend.enums.FraudFlagStatus;
import com.daswandh.backend.enums.FraudFlagType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fraud_flags")
public class FraudFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proof_id")
    private Proof proof; // null for flags that aren't about a specific proof

    @Enumerated(EnumType.STRING)
    private FraudFlagType flagType;

    private String details;

    @Enumerated(EnumType.STRING)
    private FraudFlagStatus status = FraudFlagStatus.OPEN;

    @Column(updatable = false)
    private Instant createdAt;

    protected FraudFlag() {}

    public FraudFlag(Campaign campaign, Proof proof, FraudFlagType flagType, String details) {
        this.campaign = campaign;
        this.proof = proof;
        this.flagType = flagType;
        this.details = details;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public void resolve() { this.status = FraudFlagStatus.RESOLVED; }

    public Long getId() { return id; }
    public Campaign getCampaign() { return campaign; }
    public Proof getProof() { return proof; }
    public FraudFlagType getFlagType() { return flagType; }
    public String getDetails() { return details; }
    public FraudFlagStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}