package com.amazon_backend.payment.controller;

import com.amazon_backend.payment.service.PaystackWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/webhook")
public class PaymentWebhookController {

    private final PaystackWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public PaymentWebhookController(
            PaystackWebhookService webhookService,
            ObjectMapper objectMapper
    ) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/paystack")
    public ResponseEntity<String> handlePaystackWebhook(
            @RequestHeader(
                    value = "x-paystack-signature",
                    required = false
            ) String signature,
            @RequestBody String payload
    ) {

        // 1. Validate Paystack's signature.
        if (!webhookService.isValidSignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid webhook signature");
        }

        try {
            // 2. Parse the event.
            JsonNode root = objectMapper.readTree(payload);

            String event = root.path("event").asText();

            // 3. We currently process successful transactions only.
            if (!"charge.success".equals(event)) {
                return ResponseEntity.ok("Event ignored");
            }

            // 4. Extract transaction reference.
            String reference = root.path("data")
                    .path("reference")
                    .asText(null);

            if (reference == null || reference.isBlank()) {
                return ResponseEntity.badRequest()
                        .body("Missing transaction reference");
            }

            // 5. Verify directly with Paystack and confirm payment.
            webhookService.processSuccessfulPayment(reference);

            return ResponseEntity.ok("Webhook processed");

        } catch (Exception ex) {
            // A non-2xx response allows Paystack to retry delivery.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook processing failed");
        }
    }
}