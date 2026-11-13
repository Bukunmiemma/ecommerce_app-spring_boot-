package com.amazon_backend.order.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}

//Order status	Payment status	Meaning
//PENDING	UNPAID	Order created; payment not initialized
//PENDING	PENDING	Paystack payment initialized
//PENDING	PAID	Payment successful
//CANCELLED	FAILED	Payment unsuccessful; stock restored
//CONFIRMED	PAID	Order confirmed for fulfilment