package com.daswandh.backend.service;

import com.daswandh.backend.dto.CreateDonationRequest;
import com.daswandh.backend.dto.DonationOrderResponse;
import com.daswandh.backend.dto.DonationResponse;
import com.daswandh.backend.dto.VerifyPaymentRequest;
import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.entity.Donation;
import com.daswandh.backend.entity.User;
import com.daswandh.backend.enums.CampaignStatus;
import com.daswandh.backend.enums.DonationStatus;
import com.daswandh.backend.exception.BadRequestException;
import com.daswandh.backend.exception.ConflictException;
import com.daswandh.backend.exception.ForbiddenException;
import com.daswandh.backend.exception.ResourceNotFoundException;
import com.daswandh.backend.payment.RazorpayClient;
import com.daswandh.backend.repository.CampaignRepository;
import com.daswandh.backend.repository.DonationRepository;
import com.daswandh.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
public class DonationService {

    private final DonationRepository donationRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final LedgerService ledgerService;
    private final RazorpayClient razorpayClient;

    public DonationService(DonationRepository donationRepository,
                           CampaignRepository campaignRepository,
                           UserRepository userRepository,
                           LedgerService ledgerService,
                           RazorpayClient razorpayClient) {
        this.donationRepository = donationRepository;
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.ledgerService = ledgerService;
        this.razorpayClient = razorpayClient;
    }

    // Deliberately NOT @Transactional: we call Razorpay over the network, and we must not hold a
    // database connection open while waiting for it. Each repository call below is its own short transaction.
    public DonationOrderResponse createOrder(String donorEmail, Long campaignId, CreateDonationRequest request) {
        User donor = userRepository.findByEmail(donorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new ConflictException("This campaign is not accepting donations");
        }
        if (campaign.getDeadline().isBefore(LocalDate.now())) {
            throw new ConflictException("This campaign has ended");
        }

        // 1) Ask Razorpay for an order (the amount is locked in here, on the server)
        String receipt = "don_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String orderId = razorpayClient.createOrder(request.amountPaise(), receipt,
                Map.of("campaignId", String.valueOf(campaignId)));

        // 2) Then remember it. If this save fails we only have an unpaid order at Razorpay: harmless.
        Donation donation = donationRepository.save(
                new Donation(campaign, donor, request.amountPaise(), request.anonymous(), orderId));

        return new DonationOrderResponse(donation.getId(), orderId, donation.getAmountPaise(),
                "INR", razorpayClient.getKeyId());
    }

    // Called when the browser reports "payment done".
    @Transactional
    public DonationResponse verifyPayment(String donorEmail, VerifyPaymentRequest request) {
        // Cheap check first, before touching the database: is this really signed by Razorpay?
        if (!razorpayClient.isValidPaymentSignature(request.razorpayOrderId(),
                request.razorpayPaymentId(), request.razorpaySignature())) {
            throw new BadRequestException("Invalid payment signature");
        }
        return confirmPaymentAsDonor(donorEmail, request.razorpayOrderId(), request.razorpayPaymentId());
    }

    // One transaction: lock the donation, mark it PAID and append the ledger entry. All or nothing.
    @Transactional
    public DonationResponse confirmPaymentAsDonor(String donorEmail, String orderId, String paymentId) {
        Donation donation = donationRepository.findByRazorpayOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        // Ownership check: you can only confirm your own donation.
        if (!donation.getDonor().getEmail().equals(donorEmail)) {
            throw new ForbiddenException("You can only confirm your own donations");
        }

        markPaid(donation, paymentId);
        return DonationResponse.from(donation);
    }

    // Idempotent: calling it again for an already-paid donation does nothing.
    // Step 8's webhook will call this same method.
    private void markPaid(Donation donation, String paymentId) {
        if (donation.getStatus() == DonationStatus.PAID) {
            return;
        }
        donation.setStatus(DonationStatus.PAID);          // dirty checking writes the UPDATE at commit
        donation.setRazorpayPaymentId(paymentId);
        ledgerService.recordDonation(donation);           // joins THIS transaction (MANDATORY)
    }
}