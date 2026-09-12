package com.amazon_backend.payment.repository;

import com.amazon_backend.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
        Optional<Payment> findByReference (String reference);
        //Duplicate request protection
        Optional<Payment> findByIdempotencyKey (String idempotencyKey);
        Optional<Payment> findByOrderId(Long orderId);
}
