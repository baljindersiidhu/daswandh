package com.daswandh.backend.service;

import com.daswandh.backend.exception.BadRequestException;
import com.daswandh.backend.exception.UnauthorizedException;
import com.daswandh.backend.payment.RazorpayClient;
import com.daswandh.backend.payment.RazorpayWebhookEvent;
import com.daswandh.backend.repository.WebhookEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper; // Spring Boot 4 uses Jackson 3 (package tools.jackson)

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final RazorpayClient razorpayClient;
    private final WebhookEventStore webhookEventStore;
    private final DonationService donationService;
    private final ObjectMapper objectMapper;

    public WebhookService(RazorpayClient razorpayClient,
                          WebhookEventStore webhookEventStore,
                          DonationService donationService,
                          ObjectMapper objectMapper) {
        this.razorpayClient = razorpayClient;
        this.webhookEventStore = webhookEventStore;
        this.donationService = donationService;
        this.objectMapper = objectMapper;
    }

    // ONE transaction: the "claim" insert and the business changes commit together or not at all.
    // If processing throws, the claim rolls back too, so Razorpay's retry is processed, not skipped.
    @Transactional
    public void handleRazorpayWebhook(String signature, String eventId, byte[] rawBody) {

        // 1) Is this really from Razorpay? Checked against the exact bytes we received.
        if (signature == null || !razorpayClient.isValidWebhookSignature(rawBody, signature)) {
            throw new UnauthorizedException("Invalid webhook signature");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new BadRequestException("Missing event id");
        }

        RazorpayWebhookEvent event = parse(rawBody);
        String type = event.event() == null ? "unknown" : event.event();

        // 2) Have we processed this exact event before?
        if (!webhookEventStore.claim(eventId, type)) {
            log.info("Webhook event {} was already processed, ignoring", eventId);
            return;
        }

        // 3) Do the work
        switch (type) {
            case "payment.captured" -> onPaymentCaptured(event);
            case "payment.failed" -> onPaymentFailed(event);
            default -> log.debug("Ignoring webhook event type {}", type);
        }
    }

    private void onPaymentCaptured(RazorpayWebhookEvent event) {
        RazorpayWebhookEvent.Payment payment = event.payment();
        if (payment == null || payment.orderId() == null || payment.id() == null) {
            log.warn("payment.captured webhook without payment details, ignoring");
            return;
        }
        donationService.confirmPaymentFromWebhook(payment.orderId(), payment.id(), payment.amount());
    }

    private void onPaymentFailed(RazorpayWebhookEvent event) {
        RazorpayWebhookEvent.Payment payment = event.payment();
        if (payment == null || payment.orderId() == null) {
            log.warn("payment.failed webhook without order id, ignoring");
            return;
        }
        donationService.markFailedFromWebhook(payment.orderId());
    }

    private RazorpayWebhookEvent parse(byte[] rawBody) {
        try {
            RazorpayWebhookEvent event = objectMapper.readValue(rawBody, RazorpayWebhookEvent.class);
            if (event == null) {
                throw new BadRequestException("Malformed webhook payload");
            }
            return event;
        } catch (BadRequestException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BadRequestException("Malformed webhook payload");
        }
    }
}