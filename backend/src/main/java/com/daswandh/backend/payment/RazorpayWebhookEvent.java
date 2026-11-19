package com.daswandh.backend.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Only the parts of Razorpay's payload we need. Everything else is ignored.
//   { "event": "payment.captured",
//     "payload": { "payment": { "entity": { "id": "pay_..", "order_id": "order_..", "amount": 50000 } } } }
@JsonIgnoreProperties(ignoreUnknown = true)
public record RazorpayWebhookEvent(String event, Payload payload) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payload(PaymentWrapper payment) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentWrapper(Payment entity) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payment(String id, @JsonProperty("order_id") String orderId, long amount, String status) {}

    // Null-safe shortcut: returns null if the event has no payment inside
    public Payment payment() {
        return (payload == null || payload.payment() == null) ? null : payload.payment().entity();
    }
}