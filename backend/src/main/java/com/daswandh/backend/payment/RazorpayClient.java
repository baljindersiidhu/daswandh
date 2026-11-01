package com.daswandh.backend.payment;

import com.daswandh.backend.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class RazorpayClient {

    private static final Logger log = LoggerFactory.getLogger(RazorpayClient.class);

    private final RestClient restClient;
    private final String keyId;
    private final String keySecret;

    public RazorpayClient(@Value("${app.razorpay.key-id}") String keyId,
                          @Value("${app.razorpay.key-secret}") String keySecret,
                          @Value("${app.razorpay.base-url}") String baseUrl) {

        // Safety rail: this project must never be able to touch live payments.
        if (!keyId.startsWith("rzp_test_")) {
            throw new IllegalStateException("Only Razorpay TEST keys (rzp_test_...) are allowed in this project");
        }
        this.keyId = keyId;
        this.keySecret = keySecret;

        // Timeouts: without them, a stuck Razorpay call would hang our request thread forever.
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> headers.setBasicAuth(keyId, keySecret)) // Razorpay uses Basic auth
                .build();
    }

    // Public on purpose: Razorpay Checkout in the browser needs the key id (never the secret).
    public String getKeyId() {
        return keyId;
    }

    // Creates an order and returns its id (order_xxxx). The amount is fixed here, on our server.
    public String createOrder(long amountPaise, String receipt, Map<String, String> notes) {
        Map<String, Object> payload = Map.of(
                "amount", amountPaise,
                "currency", "INR",
                "receipt", receipt,
                "notes", notes);
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null && response.get("id") instanceof String orderId) {
                return orderId;
            }
            throw new ExternalServiceException("Payment provider returned an unexpected response.");
        } catch (RestClientException e) {
            // Log the real cause for us, show the client a safe message.
            log.error("Razorpay order creation failed", e);
            throw new ExternalServiceException("Payment provider is unavailable. Please try again.");
        }
    }

    // Razorpay signs "order_id|payment_id" with our key secret.
    public boolean isValidPaymentSignature(String orderId, String paymentId, String signature) {
        String expected = HmacSigner.hmacSha256Hex(orderId + "|" + paymentId, keySecret);
        return HmacSigner.constantTimeEquals(expected, signature);
    }
}