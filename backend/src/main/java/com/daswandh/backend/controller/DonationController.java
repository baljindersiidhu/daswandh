package com.daswandh.backend.controller;

import com.daswandh.backend.dto.CreateDonationRequest;
import com.daswandh.backend.dto.DonationOrderResponse;
import com.daswandh.backend.dto.DonationResponse;
import com.daswandh.backend.dto.VerifyPaymentRequest;
import com.daswandh.backend.service.DonationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DonationController {

    private final DonationService donationService;

    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    @PostMapping("/api/campaigns/{campaignId}/donations")
    @ResponseStatus(HttpStatus.CREATED)
    public DonationOrderResponse createOrder(@AuthenticationPrincipal UserDetails principal,
                                             @PathVariable Long campaignId,
                                             @Valid @RequestBody CreateDonationRequest request) {
        return donationService.createOrder(principal.getUsername(), campaignId, request);
    }

    @PostMapping("/api/donations/verify")
    public DonationResponse verify(@AuthenticationPrincipal UserDetails principal,
                                   @Valid @RequestBody VerifyPaymentRequest request) {
        return donationService.verifyPayment(principal.getUsername(), request);
    }
}