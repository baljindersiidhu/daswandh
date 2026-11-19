package com.daswandh.backend.controller;

import com.daswandh.backend.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    // Public URL (no JWT). The signature is what proves the caller is Razorpay.
    // We take the body as raw bytes, because the signature is computed over those exact bytes.
    @PostMapping("/api/webhooks/razorpay")
    public ResponseEntity<Void> razorpay(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId,
            @RequestBody byte[] rawBody) {

        webhookService.handleRazorpayWebhook(signature, eventId, rawBody);
        return ResponseEntity.ok().build(); // any 2xx tells Razorpay "got it, stop retrying"
    }
}