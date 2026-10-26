package com.daswandh.backend.entity;

import com.daswandh.backend.enums.DonationStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "donations")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id")
    private User donor;

    private long amountPaise;

    @Enumerated(EnumType.STRING)
    private DonationStatus status = DonationStatus.CREATED;

    @Column(name = "is_anonymous")
    private boolean anonymous;

    private String razorpayOrderId;
    private String razorpayPaymentId; // filled in when the payment succeeds

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    protected Donation() {}

    public Donation(Campaign campaign, User donor, long amountPaise, boolean anonymous, String razorpayOrderId) {
        this.campaign = campaign;
        this.donor = donor;
        this.amountPaise = amountPaise;
        this.anonymous = anonymous;
        this.razorpayOrderId = razorpayOrderId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Campaign getCampaign() { return campaign; }
    public User getDonor() { return donor; }
    public long getAmountPaise() { return amountPaise; }
    public DonationStatus getStatus() { return status; }
    public boolean isAnonymous() { return anonymous; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(DonationStatus status) { this.status = status; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
}