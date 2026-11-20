package com.amazon_backend.payment.controller;

import com.amazon_backend.payment.service.PaystackWebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/payments/webhook")
public class PaymentWebhookController {

    private final PaystackWebhookService webhookService;
    private final JsonMapper jsonMapper;

    public PaymentWebhookController(
            PaystackWebhookService webhookService,
            JsonMapper jsonMapper
    ) {
        this.webhookService = webhookService;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping("/paystack")
    public ResponseEntity<String> handlePaystackWebhook(
            @RequestHeader(
                    value = "x-paystack-signature",
                    required = false
            ) String signature,
            @RequestBody String payload
    ) {

        // 1. Validate Paystack signature.
        if (!webhookService.isValidSignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid webhook signature");
        }

        try {
            // 2. Parse webhook JSON.
            JsonNode root = jsonMapper.readTree(payload);

            String event = root.path("event").asString();

            // 3. Process successful payments only.
            if (!"charge.success".equals(event)) {
                return ResponseEntity.ok("Event ignored");
            }

            // 4. Extract transaction reference.
            String reference = root.path("data")
                    .path("reference")
                    .asString();

            if (reference.isBlank()) {
                return ResponseEntity.badRequest()
                        .body("Missing transaction reference");
            }

            // 5. Verify and process payment.
            webhookService.processSuccessfulPayment(reference);

            return ResponseEntity.ok("Webhook processed");

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook processing failed");
        }
    }
}