package com.daswandh.backend.entity;

import com.daswandh.backend.enums.ProofStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "proofs")
public class Proof {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by") // explicit, because the default would be uploaded_by_id
    private User uploadedBy;

    private String fileUrl;
    private String imageHash; // SHA-256 of the file, used for duplicate detection
    private String description;
    private long amountSpentPaise;

    @Enumerated(EnumType.STRING)
    private ProofStatus status = ProofStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private String reviewNote;
    private Instant reviewedAt;

    @Column(updatable = false)
    private Instant createdAt;

    protected Proof() {}

    public Proof(Milestone milestone, User uploadedBy, String fileUrl, String imageHash,
                 String description, long amountSpentPaise) {
        this.milestone = milestone;
        this.uploadedBy = uploadedBy;
        this.fileUrl = fileUrl;
        this.imageHash = imageHash;
        this.description = description;
        this.amountSpentPaise = amountSpentPaise;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    // One method for the admin decision keeps the review fields consistent.
    public void review(ProofStatus decision, User admin, String note) {
        this.status = decision;
        this.reviewedBy = admin;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Milestone getMilestone() { return milestone; }
    public User getUploadedBy() { return uploadedBy; }
    public String getFileUrl() { return fileUrl; }
    public String getImageHash() { return imageHash; }
    public String getDescription() { return description; }
    public long getAmountSpentPaise() { return amountSpentPaise; }
    public ProofStatus getStatus() { return status; }
    public User getReviewedBy() { return reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
}