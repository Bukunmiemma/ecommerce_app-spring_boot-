package com.amazon_backend.order.service;
import org.springframework.stereotype.Service;
import com.amazon_backend.order.entity.Order;
import com.amazon_backend.order.entity.OrderItem;
import com.amazon_backend.order.entity.OrderStatus;
import com.amazon_backend.order.entity.PaymentStatus;
import com.amazon_backend.order.repositories.OrderRepository;
import com.amazon_backend.product.entity.Product;
import com.amazon_backend.product.repository.ProductRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class OrderInventoryService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderInventoryService(
            OrderRepository orderRepository,
            ProductRepository productRepository) {

        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    /**
     * Restores stock reserved by an order whose payment
     * has definitively failed or expired.
     *
     * For UNPAID orders, pass null as the reference.
     * For PENDING orders, pass the matching Paystack
     * reference after server-side verification confirms
     * a terminal unsuccessful payment.
     *
     * Returns false if the order was already cancelled.
     */
    @Transactional
    public boolean restoreReservedStock(
            Long orderId,
            String verifiedPaymentReference,
            PaymentStatus finalPaymentStatus
            ) {

        // Only FAILED and EXPIRED are valid outcomes
        if (finalPaymentStatus != PaymentStatus.FAILED
                && finalPaymentStatus != PaymentStatus.EXPIRED) {

            throw new IllegalArgumentException(
                    "Invalid payment outcome for stock restoration");
        }

        // 1. Lock the order.
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

// 2. Prevent duplicate stock restoration.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return false;
        }

// 3. Never expire an order before its actual deadline.
        if (finalPaymentStatus == PaymentStatus.EXPIRED
                && (order.getPaymentExpiresAt() == null
                || order.getPaymentExpiresAt().isAfter(LocalDateTime.now()))) {

            throw new IllegalStateException(
                    "Order payment expiration deadline has not passed");
        }

        // 4. Never restore stock for a successful payment.
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Cannot restore stock for a paid order");
        }

        // 5. Validate payment state and reference.
        if (order.getPaymentStatus() == PaymentStatus.UNPAID) {


            if (finalPaymentStatus != PaymentStatus.EXPIRED) {
                throw new IllegalStateException(
                        "An unpaid order can only expire");
            }

            if (order.getPaymentReference() != null) {
                throw new IllegalStateException(
                        "Unpaid order unexpectedly has a payment reference");
            }

            if (verifiedPaymentReference != null) {
                throw new IllegalArgumentException(
                        "Unexpected payment reference for unpaid order");
            }

        } else if (order.getPaymentStatus() == PaymentStatus.PENDING) {

            if (verifiedPaymentReference == null
                    || verifiedPaymentReference.isBlank()
                    || !verifiedPaymentReference.equals(
                    order.getPaymentReference())) {

                throw new IllegalStateException(
                        "Payment reference verification failed");
            }

        } else {
            throw new IllegalStateException(
                    "Order payment state does not permit stock restoration");
        }

        // 6. Sort order items to maintain consistent product lock order.
        List<OrderItem> items = order.getItems()
                .stream()
                .sorted(Comparator.comparing(
                        item -> item.getProduct().getId()))
                .toList();

        // 7. Lock each product and restore its reserved quantity.
        for (OrderItem item : items) {

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalStateException(
                        "Invalid quantity in order item");
            }

            Long productId = item.getProduct().getId();

            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Product not found: " + productId));

            if (product.getStockQuantity() == null
                    || product.getStockQuantity() < 0) {

                throw new IllegalStateException(
                        "Invalid stock quantity for product: " + productId);
            }

            // Math.addExact prevents integer overflow.
            int restoredStock = Math.addExact(
                    product.getStockQuantity(),
                    item.getQuantity());

            product.setStockQuantity(restoredStock);
        }

        // 8. Update order state in the same transaction.
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(finalPaymentStatus);

        return true;
    }
}

//Order-level pessimistic locking.
//Product-level pessimistic locking.
//Duplicate-restoration protection.
//Transactional stock restoration.
//Payment-state validation.