package com.amazon_backend.order.entity;

//This tracks the financial state of the order.
public enum PaymentStatus {
    UNPAID,
    PENDING,
    PAID,
    FAILED,
    REFUNDED,
    EXPIRED,
    REFUND_PENDING
}

//EXPIRED,
//REFUND_PENDING This allows us to represent an expired order separately from a failed
// transaction and a successful transaction awaiting a refund.
//UNPAID – Checkout created, payment not initialized.
//PENDING – Payment initialization completed; awaiting confirmation.
//PAID – Payment successfully verified.
//FAILED – Payment failed.
//REFUNDED – A previously successful payment was refunded.