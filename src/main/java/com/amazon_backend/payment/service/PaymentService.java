package com.amazon_backend.payment.service;

import com.amazon_backend.payment.dto.PaymentInitializeRequest;
import com.amazon_backend.payment.dto.PaymentInitializeResponse;

public interface PaymentService {
    PaymentInitializeResponse initializePayment(
            String email,
            PaymentInitializeRequest request
    );
}
