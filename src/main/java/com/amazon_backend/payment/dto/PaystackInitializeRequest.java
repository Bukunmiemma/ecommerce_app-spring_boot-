package com.amazon_backend.payment.dto;

import java.util.Map;

//This is strictly for communication between our spring boot backend and paystack

public class PaystackInitializeRequest {
//Why Long amount instead of BigDecimal? Your database stores:
//₦1,200,000.00 as a BigDecimal.But Paystack expects the amount in the currency's subunit.
//For Nigerian naira: ₦1,200,000 × 100 = 120,000,000 kobo
//So our service will eventually convert: order.getTotalAmount() into: 120000000 before sending it to Paystack.
    private Long amount;
    private String email;
    private String reference;
    private String currency;
    private Map<String, Object> metadata;

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
