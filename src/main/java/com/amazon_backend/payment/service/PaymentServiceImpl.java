package com.amazon_backend.payment.service;
import com.amazon_backend.auth.entity.User;
import com.amazon_backend.auth.repository.UserRepository;
import com.amazon_backend.order.entity.Order;
import com.amazon_backend.order.entity.OrderStatus;
import com.amazon_backend.order.repositories.OrderRepository;
import com.amazon_backend.payment.dto.PaymentInitializeRequest;
import com.amazon_backend.payment.dto.PaymentInitializeResponse;
import com.amazon_backend.payment.dto.PaystackInitializeRequest;
import com.amazon_backend.payment.dto.PaystackInitializeResponse;
import com.amazon_backend.payment.entity.Payment;
import com.amazon_backend.payment.entity.PaymentStatus;
import com.amazon_backend.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestClient paystackRestClient;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              UserRepository userRepository,
                              RestClient paystackRestClient) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.paystackRestClient = paystackRestClient;
    }
    @Override
    @Transactional
    public PaymentInitializeResponse initializePayment(
            String email,
            PaymentInitializeRequest request) {

        // 1. Find the authenticated user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Find the order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 3. Make sure the order belongs to this user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You are not authorized to pay for this order"
            );
        }

        // 4. Only pending orders can be paid for
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException(
                    "Order is not available for payment"
            );
        }

        // 5. Check whether this order already has a payment
        Payment existingPayment = paymentRepository
                .findByOrderId(order.getId())
                .orElse(null);

        if (existingPayment != null) {

            if (existingPayment.getStatus() == PaymentStatus.SUCCESS) {
                throw new RuntimeException(
                        "Order has already been paid for"
                );
            }

            PaymentInitializeResponse response =
                    new PaymentInitializeResponse();

            response.setReference(existingPayment.getReference());

            return response;
        }

        // 6. Get the trusted amount directly from the order
        BigDecimal amount = order.getTotalAmount();

        // 7. Currency for our current Nigerian store
        String currency = "NGN";

        // 8. Generate a unique payment reference
        String reference =
                "ORDER-" + order.getId() + "-" + UUID.randomUUID();

        // 9. Create our Payment record
        Payment payment = new Payment();

        payment.setOrder(order);
        payment.setReference(reference);
        payment.setIdempotencyKey(UUID.randomUUID().toString());
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        // 10. Convert Naira to Kobo
        long amountInKobo = amount
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        // 11. Build Paystack request
        PaystackInitializeRequest paystackRequest =
                new PaystackInitializeRequest();

        paystackRequest.setAmount(amountInKobo);
        paystackRequest.setEmail(user.getEmail());
        paystackRequest.setReference(reference);
        paystackRequest.setCurrency(currency);

        // 12. Send request to Paystack
        PaystackInitializeResponse paystackResponse =
                paystackRestClient
                        .post()
                        .uri("/transaction/initialize")
                        .body(paystackRequest)
                        .retrieve()
                        .body(PaystackInitializeResponse.class);

        // 13. Check Paystack response
        if (paystackResponse == null
                || !paystackResponse.isStatus()
                || paystackResponse.getData() == null) {

            throw new RuntimeException(
                    "Paystack payment initialization failed"
            );
        }

        // 14. Save payment only after Paystack accepts initialization
        paymentRepository.save(payment);

        // 15. Prepare response for Flutter
        PaymentInitializeResponse response =
                new PaymentInitializeResponse();

        response.setAuthorizationUrl(
                paystackResponse.getData().getAuthorization_url()
        );

        response.setAccessCode(
                paystackResponse.getData().getAccess_code()
        );

        response.setReference(
                paystackResponse.getData().getReference()
        );

        return response;
    }
}
