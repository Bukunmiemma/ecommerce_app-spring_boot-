package com.amazon_backend.payment.service;

import com.amazon_backend.payment.dto.PaymentInitializeRequest;
import com.amazon_backend.payment.dto.PaymentInitializeResponse;

public interface PaymentService {
    PaymentInitializeResponse initializePayment(
            String email,
            PaymentInitializeRequest request
    );

    void verifyPayment(String email, String reference);
    void verifyPaymentFromWebhook(String reference);
}


//When Flutter receives the Paystack callback, it will send the transaction reference to our backend.
//Our backend will then contact Paystack directly to confirm whether the transaction succeeded.
//Important: Flutter must never be allowed to tell our backend that a payment succeeded without independent verification.