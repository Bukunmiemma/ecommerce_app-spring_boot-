package com.amazon_backend.payment.entity;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED
}
//This tracks the Paystack transaction itself.