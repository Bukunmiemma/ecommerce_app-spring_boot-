package com.amazon_backend.payment.controller;

import com.amazon_backend.payment.dto.PaymentInitializeRequest;
import com.amazon_backend.payment.dto.PaymentInitializeResponse;
import com.amazon_backend.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initialize")
    public ResponseEntity<PaymentInitializeResponse> initializePayment(
            @RequestBody PaymentInitializeRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        PaymentInitializeResponse response =
                paymentService.initializePayment(email, request);

        return ResponseEntity.ok(response);
    }
}
