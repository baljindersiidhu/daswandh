package com.daswandh.backend.dto;

// Everything the browser needs to open Razorpay Checkout.
public record DonationOrderResponse(Long donationId, String razorpayOrderId, long amountPaise,
                                    String currency, String razorpayKeyId) {}