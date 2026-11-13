package com.amazon_backend.payment.controller;

import com.amazon_backend.payment.dto.PaymentInitializeRequest;
import com.amazon_backend.payment.dto.PaymentInitializeResponse;
import com.amazon_backend.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    //Receives the order ID and asks Paystack to initialize payment.
    @PostMapping("/initialize")
    public ResponseEntity<PaymentInitializeResponse> initializePayment(
            @RequestBody PaymentInitializeRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        PaymentInitializeResponse response =
                paymentService.initializePayment(email, request);

        return ResponseEntity.ok(response);
    }

    //Uses the authenticated customer's email and transaction reference to initiate server-side verification.
    @PostMapping("/verify/{reference}")
    public ResponseEntity<String> verifyPayment(
            @PathVariable String reference,
            Authentication authentication) {

        paymentService.verifyPayment(
                authentication.getName(),
                reference
        );

        return ResponseEntity.ok(
                "Payment verified successfully"
        );
    }


}
