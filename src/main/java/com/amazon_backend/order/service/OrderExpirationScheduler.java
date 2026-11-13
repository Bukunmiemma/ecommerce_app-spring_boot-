package com.amazon_backend.order.service;
import com.amazon_backend.order.entity.Order;
import com.amazon_backend.order.entity.OrderStatus;
import com.amazon_backend.order.entity.PaymentStatus;
import com.amazon_backend.order.repositories.OrderRepository;
import com.amazon_backend.payment.service.PaymentServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderExpirationScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(OrderExpirationScheduler.class);

    private final OrderRepository orderRepository;
    private final OrderInventoryService orderInventoryService;
    private final PaymentServiceImpl paymentService;

    public OrderExpirationScheduler(
            OrderRepository orderRepository,
            OrderInventoryService orderInventoryService,
            PaymentServiceImpl paymentService
    ) {
        this.orderRepository = orderRepository;
        this.orderInventoryService = orderInventoryService;
        this.paymentService = paymentService;
    }

    // Run every 60 seconds.
    @Scheduled(fixedDelay = 60000)
    public void expireOrders() {

        List<Order> expiredOrders =
                orderRepository
                        .findTop100ByStatusAndPaymentStatusInAndPaymentExpiresAtBefore(
                                OrderStatus.PENDING,
                                List.of(
                                        PaymentStatus.UNPAID,
                                        PaymentStatus.PENDING
                                ),
                                LocalDateTime.now()
                        );

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info(
                "Found {} expired orders to process",
                expiredOrders.size()
        );

        for (Order order : expiredOrders) {

            try {

                // Handle orders without payment attempts.
                if (order.getPaymentStatus() == PaymentStatus.UNPAID) {

                    orderInventoryService.restoreReservedStock(
                            order.getId(),
                            null,
                            PaymentStatus.EXPIRED
                    );

                    log.info(
                            "Expired unpaid order: {}",
                            order.getId()
                    );

                    continue;
                }

                // Handle orders with existing payment attempts.
                if (order.getPaymentStatus() == PaymentStatus.PENDING) {

                    String reference = order.getPaymentReference();

                    if (reference == null || reference.isBlank()) {
                        log.warn(
                                "Pending order {} has no payment reference",
                                order.getId()
                        );

                        continue;
                    }

                    paymentService.verifyPaymentForExpiration(reference);

                    log.info(
                            "Processed expiration verification for order {}",
                            order.getId()
                    );
                }

            } catch (Exception exception) {

                // One problematic order must not stop the entire batch.
                log.error(
                        "Failed to process expired order {}: {}",
                        order.getId(),
                        exception.getMessage(),
                        exception
                );
            }
        }
    }
}
