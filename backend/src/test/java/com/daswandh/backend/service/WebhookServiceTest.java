package com.daswandh.backend.service;

import com.daswandh.backend.exception.BadRequestException;
import com.daswandh.backend.exception.UnauthorizedException;
import com.daswandh.backend.payment.RazorpayClient;
import com.daswandh.backend.repository.WebhookEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock private RazorpayClient razorpayClient;
    @Mock private WebhookEventStore webhookEventStore;
    @Mock private DonationService donationService;

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        // A real JSON mapper, so we also test that the payload is parsed correctly
        webhookService = new WebhookService(razorpayClient, webhookEventStore, donationService,
                JsonMapper.builder().build());
    }

    private byte[] body(String eventType) {
        String json = """
                {"entity":"event","event":"%s","payload":{"payment":{"entity":
                {"id":"pay_1","order_id":"order_1","amount":50000,"currency":"INR","status":"captured"}}}}
                """.formatted(eventType);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private void signatureIsValid() {
        when(razorpayClient.isValidWebhookSignature(any(byte[].class), eq("sig"))).thenReturn(true);
    }

    @Test
    void forgedOrMissingSignatureIsRejectedBeforeAnythingElseHappens() {
        when(razorpayClient.isValidWebhookSignature(any(byte[].class), eq("forged"))).thenReturn(false);

        assertThatThrownBy(() -> webhookService.handleRazorpayWebhook("forged", "evt_1", body("payment.captured")))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> webhookService.handleRazorpayWebhook(null, "evt_1", body("payment.captured")))
                .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(webhookEventStore, donationService);
    }

    @Test
    void capturedPaymentIsConfirmed() {
        signatureIsValid();
        when(webhookEventStore.claim("evt_1", "payment.captured")).thenReturn(true);

        webhookService.handleRazorpayWebhook("sig", "evt_1", body("payment.captured"));

        verify(donationService).confirmPaymentFromWebhook("order_1", "pay_1", 50_000L);
    }

    @Test
    void duplicateEventIsIgnored() {
        signatureIsValid();
        when(webhookEventStore.claim("evt_1", "payment.captured")).thenReturn(false);

        webhookService.handleRazorpayWebhook("sig", "evt_1", body("payment.captured"));

        verify(donationService, never()).confirmPaymentFromWebhook(anyString(), anyString(), anyLong());
    }

    @Test
    void failedPaymentIsMarkedFailed() {
        signatureIsValid();
        when(webhookEventStore.claim("evt_2", "payment.failed")).thenReturn(true);

        webhookService.handleRazorpayWebhook("sig", "evt_2", body("payment.failed"));

        verify(donationService).markFailedFromWebhook("order_1");
    }

    @Test
    void unknownEventTypesAreAcceptedButDoNothing() {
        signatureIsValid();
        when(webhookEventStore.claim("evt_3", "refund.created")).thenReturn(true);

        webhookService.handleRazorpayWebhook("sig", "evt_3", body("refund.created"));

        verifyNoInteractions(donationService);
    }

    @Test
    void malformedJsonIsABadRequestAndIsNotClaimed() {
        signatureIsValid();

        assertThatThrownBy(() -> webhookService.handleRazorpayWebhook("sig", "evt_4",
                "this is not json".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(webhookEventStore, donationService);
    }

    @Test
    void missingEventIdIsABadRequest() {
        signatureIsValid();

        assertThatThrownBy(() -> webhookService.handleRazorpayWebhook("sig", " ", body("payment.captured")))
                .isInstanceOf(BadRequestException.class);
    }
}