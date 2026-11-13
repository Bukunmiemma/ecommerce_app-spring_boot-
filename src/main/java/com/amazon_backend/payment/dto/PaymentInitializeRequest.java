package com.amazon_backend.payment.dto;

//Request from the flutter app
public class PaymentInitializeRequest {
    private Long orderId;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
