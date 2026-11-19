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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DonationService {

    private static final Logger log = LoggerFactory.getLogger(DonationService.class);

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

    // Deliberately NOT @Transactional: we call Razorpay over the network and must not hold
    // a database connection open while waiting for it.
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

        String receipt = "don_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String orderId = razorpayClient.createOrder(request.amountPaise(), receipt,
                Map.of("campaignId", String.valueOf(campaignId)));

        Donation donation = donationRepository.save(
                new Donation(campaign, donor, request.amountPaise(), request.anonymous(), orderId));

        return new DonationOrderResponse(donation.getId(), orderId, donation.getAmountPaise(),
                "INR", razorpayClient.getKeyId());
    }

    // Path 1: the browser says "payment done"
    public DonationResponse verifyPayment(String donorEmail, VerifyPaymentRequest request) {
        if (!razorpayClient.isValidPaymentSignature(request.razorpayOrderId(),
                request.razorpayPaymentId(), request.razorpaySignature())) {
            throw new BadRequestException("Invalid payment signature");
        }
        return confirmPaymentAsDonor(donorEmail, request.razorpayOrderId(), request.razorpayPaymentId());
    }

    @Transactional
    public DonationResponse confirmPaymentAsDonor(String donorEmail, String orderId, String paymentId) {
        Donation donation = donationRepository.findByRazorpayOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        if (!donation.getDonor().getEmail().equals(donorEmail)) {
            throw new ForbiddenException("You can only confirm your own donations");
        }

        markPaid(donation, paymentId);
        return DonationResponse.from(donation);
    }

    // Path 2: Razorpay's server says "payment captured". No logged-in user here, so no ownership check:
    // the webhook signature was already verified by WebhookService.
    @Transactional
    public void confirmPaymentFromWebhook(String orderId, String paymentId, long amountPaise) {
        Optional<Donation> found = donationRepository.findByRazorpayOrderIdForUpdate(orderId);
        if (found.isEmpty()) {
            // Not one of ours (or not saved yet). Retrying won't help, so we just note it.
            log.warn("Webhook for unknown order {} ignored", orderId);
            return;
        }
        Donation donation = found.get();

        // Defensive: the order was created by us with a fixed amount, so this should never happen.
        if (donation.getAmountPaise() != amountPaise) {
            log.error("Webhook amount mismatch for order {}: expected {} paise, got {} paise. Not crediting.",
                    orderId, donation.getAmountPaise(), amountPaise);
            return;
        }
        markPaid(donation, paymentId);
    }

    @Transactional
    public void markFailedFromWebhook(String orderId) {
        donationRepository.findByRazorpayOrderIdForUpdate(orderId).ifPresent(donation -> {
            // Only CREATED can become FAILED. A PAID donation is never downgraded by a late failure event.
            if (donation.getStatus() == DonationStatus.CREATED) {
                donation.setStatus(DonationStatus.FAILED);
            }
        });
    }

    // Idempotent: both paths end here, and a second call for a paid donation does nothing.
    // A FAILED donation CAN become PAID: Razorpay lets a donor retry the same order.
    private void markPaid(Donation donation, String paymentId) {
        if (donation.getStatus() == DonationStatus.PAID) {
            return;
        }
        donation.setStatus(DonationStatus.PAID);
        donation.setRazorpayPaymentId(paymentId);
        ledgerService.recordDonation(donation); // joins THIS transaction (MANDATORY)
    }
}