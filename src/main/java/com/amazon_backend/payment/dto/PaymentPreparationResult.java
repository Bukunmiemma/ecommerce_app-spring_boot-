package com.amazon_backend.payment.dto;

import java.math.BigDecimal;

public record PaymentPreparationResult(
        Long paymentId,
        String reference,
        String email,
        String currency,
        BigDecimal amount,
        String authorizationUrl,
        String accessCode,
                String initializationLeaseToken

) {

}
