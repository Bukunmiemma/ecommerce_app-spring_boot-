package com.amazon_backend.payment.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class PaystackWebhookService {

    private final PaymentService paymentService;
    private final String secretKey;

    public PaystackWebhookService(
            PaymentService paymentService,
            @Value("${paystack.secret.key}") String secretKey
    ) {
        this.paymentService = paymentService;
        this.secretKey = secretKey;
    }

    public boolean isValidSignature(String payload, String signature) {

        if (payload == null || signature == null || signature.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA512");

            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );

            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            String expectedSignature = HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.US_ASCII),
                    signature.getBytes(StandardCharsets.US_ASCII)
            );

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to validate Paystack webhook signature",
                    ex
            );
        }
    }

    public void processSuccessfulPayment(String reference) {

        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment reference is missing"
            );
        }

        // Independently verify the transaction with Paystack.
        paymentService.verifyPaymentFromWebhook(reference);
    }
}