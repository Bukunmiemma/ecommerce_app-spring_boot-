package com.amazon_backend.payment.entity;
import com.amazon_backend.order.entity.Order;
import com.amazon_backend.order.entity.OrderStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "order_id", nullable = false,unique = true)
    private Order order;

//This will be our transaction reference sent to paystack
    //Paystack's initialize Transaction API accepts a unique reference
    //and returns the reference along with the checkout URL and access code
    @Column(nullable = false,unique = true)
    private  String reference;

    //This protects our payment initialization against duplicate requests
    @Column(nullable = false,unique = true)
    private  String idempotencyKey;

   // This is the amount we're expecting to receive
   //Paystack expects the transaction amount in the currency's subunit
   //e.g for NGN that means naira -> kobo when sending the paystack API request
    //The conversion will be done in the Paystack service not in the database
    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,unique = true)
    private  PaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
