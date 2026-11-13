package com.amazon_backend.payment.service;
import com.amazon_backend.auth.entity.User;
import com.amazon_backend.auth.repository.UserRepository;
import com.amazon_backend.order.entity.Order;
import com.amazon_backend.order.entity.OrderStatus;
import com.amazon_backend.order.entity.PaymentStatus;
import com.amazon_backend.order.repositories.OrderRepository;
import com.amazon_backend.order.service.OrderInventoryService;
import com.amazon_backend.payment.dto.PaymentPreparationResult;
import com.amazon_backend.payment.entity.Payment;
import com.amazon_backend.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentPreparationService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderInventoryService orderInventoryService;

    public PaymentPreparationService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            OrderInventoryService orderInventoryService
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderInventoryService =orderInventoryService;
    }

    @Transactional
    public PaymentPreparationResult preparePayment(
            String email,
            Long orderId
    ) {


        // 1. Find authenticated user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // 2. Lock the order before modifying payment information
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        // 3. Verify ownership
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You are not authorized to pay for this order"
            );
        }

        // 4. Check order status
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException(
                    "Order is not available for payment"
            );
        }

        // 5. Check payment status on the order
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException(
                    "Order has already been paid for"
            );
        }

        if (order.getPaymentStatus() == PaymentStatus.FAILED
                || order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new RuntimeException(
                    "This order cannot be paid for"
            );
        }

        // 6. Check payment expiration
        if (order.getPaymentExpiresAt() == null
                || !order.getPaymentExpiresAt().isAfter(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Payment window for this order has expired"
            );
        }

        // 7. Get trusted amount from the database
        BigDecimal amount = order.getTotalAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid order amount");
        }

        String currency = "NGN";

        // 8. Check whether a payment already exists
        Payment existingPayment = paymentRepository
                .findByOrderIdForUpdate(order.getId())
                .orElse(null);

        if (existingPayment != null) {

            if (existingPayment.getStatus() == com.amazon_backend.payment.entity.PaymentStatus.SUCCESS) {
                throw new RuntimeException(
                        "Order has already been paid for"
                );
            }

            if (existingPayment.getStatus() == com.amazon_backend.payment.entity.PaymentStatus.FAILED
                    || existingPayment.getStatus() == com.amazon_backend.payment.entity.PaymentStatus.CANCELLED) {
                throw new RuntimeException(
                        "This payment attempt cannot be reused"
                );
            }

            // Reuse the existing reference
            order.setPaymentReference(existingPayment.getReference());
            order.setPaymentStatus(PaymentStatus.PENDING);

            return new PaymentPreparationResult(
                    existingPayment.getPaymentId(),
                    existingPayment.getReference(),
                    user.getEmail(),
                    existingPayment.getCurrency(),
                    existingPayment.getAmount(),
                    existingPayment.getAuthorizationUrl(),
                    existingPayment.getAccessCode(),
                    existingPayment.getInitializationLeaseToken()
            );
        }

        // 9. Create a stable payment reference
        String reference =
                "ORDER-" + order.getId() + "-" + UUID.randomUUID();

        // 10. Create payment record
        Payment payment = new Payment();

        payment.setOrder(order);
        payment.setReference(reference);
        payment.setIdempotencyKey(UUID.randomUUID().toString());
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(
                com.amazon_backend.payment.entity.PaymentStatus.PENDING
        );

        LocalDateTime now = LocalDateTime.now();

        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        // Generate initialization lease token
        String initializationLeaseToken = UUID.randomUUID().toString();

        payment.setInitializationLeaseToken(initializationLeaseToken);
        payment.setInitializationLeaseUntil(now.plusMinutes(2));

        paymentRepository.saveAndFlush(payment);

        // 11. Update the order
        order.setPaymentReference(reference);
        order.setPaymentStatus(PaymentStatus.PENDING);


        return new PaymentPreparationResult(
                payment.getPaymentId(),
                reference,
                user.getEmail(),
                currency,
                amount,
                null,
                null,
                initializationLeaseToken

        );
    }

    @Transactional
    public void saveAuthorizationDetails(
            String reference,
            String leaseToken,
            String authorizationUrl,
            String accessCode
    ) {

        Payment payment = paymentRepository
                .findByReferenceForUpdate(reference)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        if (payment.getStatus()
                != com.amazon_backend.payment.entity.PaymentStatus.PENDING) {
            throw new RuntimeException(
                    "Payment is no longer awaiting initialization"
            );
        }
        if (leaseToken == null
                || !leaseToken.equals(payment.getInitializationLeaseToken())) {

            throw new RuntimeException(
                    "Invalid or expired initialization lease"
            );
        }

        if (payment.getInitializationLeaseUntil() == null
                || !payment.getInitializationLeaseUntil().isAfter(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Payment initialization lease has expired"
            );
        }

        payment.setAuthorizationUrl(authorizationUrl);
        payment.setAccessCode(accessCode);

        // Clear initialization lease after successful initialization
        payment.setInitializationLeaseToken(null);
        payment.setInitializationLeaseUntil(null);
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        //Notice that the method now checks:
        //- Payment status.
        //- Initialization token.
        //- Lease expiration.
        //After successfully saving the authorization details, it clears the lease.


    }

    @Transactional
    public void confirmSuccessfulPayment(
            String email,
            String reference,
            Long amountInKobo,
            String currency,
            String customerEmail
    ) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Payment reference is required");
        }
        // 1. Find the payment
        Payment existingPayment = paymentRepository
                .findByReference(reference)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        Long orderId = existingPayment.getOrder().getId();

        // 2. Lock the order first
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        // 3. Lock the payment
        Payment payment = paymentRepository
                .findByReferenceForUpdate(reference)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        // 4. Validate the amount in kobo
        long expectedAmountInKobo = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        if (amountInKobo == null || amountInKobo != expectedAmountInKobo) {
            throw new RuntimeException("Payment amount mismatch");
        }

        // 5. Verify ownership
        if (!order.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException(
                    "You are not authorized to verify this payment");
        }

        // 6. Verify Paystack transaction details
        if (!reference.equals(payment.getReference())) {
            throw new RuntimeException("Payment reference mismatch");
        }


//This prevents a NullPointerException if Paystack returns missing currency information.
        if (currency == null
                || payment.getCurrency() == null
                || !payment.getCurrency().equalsIgnoreCase(currency)) {

            throw new RuntimeException("Payment currency mismatch");
        }

        if (customerEmail == null
                || !order.getUser().getEmail()
                .equalsIgnoreCase(customerEmail)) {

            throw new RuntimeException("Customer email mismatch");
        }

        // 7. Handle repeated verification safely
        if (payment.getStatus()
                == com.amazon_backend.payment.entity.PaymentStatus.SUCCESS) {

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                return;
            }

            throw new IllegalStateException(
                    "Payment and order statuses are inconsistent");
        }

        // 8. Only pending payments can become successful
        if (payment.getStatus()
                != com.amazon_backend.payment.entity.PaymentStatus.PENDING) {

            throw new RuntimeException(
                    "Payment is not awaiting verification");
        }

        if (order.getStatus() != OrderStatus.PENDING
                || order.getPaymentStatus() != PaymentStatus.PENDING
                || !reference.equals(order.getPaymentReference())) {

            throw new RuntimeException(
                    "Order is not in a valid state for payment confirmation");
        }

        // 9. Confirm payment
        payment.setStatus(
                com.amazon_backend.payment.entity.PaymentStatus.SUCCESS);

        payment.setUpdatedAt(LocalDateTime.now());

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.CONFIRMED);

        paymentRepository.save(payment);

        //What this method protects
        //- A customer cannot verify another customer's payment.
        //- A different amount cannot be accepted.
        //- A different currency cannot be accepted.
        //- A different customer email cannot be accepted.
        //- A payment cannot be confirmed twice and processed twice.
        //- Stock is not restored after successful payment.
        //It also locks the order before the payment, maintaining the locking order used by your payment preparation flow.
    }

    @Transactional
    public void confirmFailedPayment(
            String email,
            String reference,
            Long amountInKobo,
            String currency,
            String customerEmail
    ) {

        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment reference is required"
            );
        }

        // 1. Find payment
        Payment existingPayment = paymentRepository
                .findByReference(reference)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        Long orderId = existingPayment.getOrder().getId();

        // 2. Lock order first
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        // 3. Lock payment
        Payment payment = paymentRepository
                .findByReferenceForUpdate(reference)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        // 4. Validate amount
        long expectedAmountInKobo = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        if (amountInKobo == null
                || amountInKobo != expectedAmountInKobo) {

            throw new RuntimeException("Payment amount mismatch");
        }

        // 5. Verify ownership
        if (!order.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException(
                    "You are not authorized to verify this payment");
        }

        // 6. Verify reference
        if (!reference.equals(payment.getReference())
                || !reference.equals(order.getPaymentReference())) {

            throw new RuntimeException("Payment reference mismatch");
        }

        // 7. Verify currency
        if (currency == null
                || payment.getCurrency() == null
                || !payment.getCurrency().equalsIgnoreCase(currency)) {

            throw new RuntimeException("Payment currency mismatch");
        }

        // 8. Verify customer email
        if (customerEmail == null
                || !order.getUser().getEmail()
                .equalsIgnoreCase(customerEmail)) {

            throw new RuntimeException("Customer email mismatch");
        }

        // 9. Prevent duplicate failed-payment processing
        if (payment.getStatus()
                == com.amazon_backend.payment.entity.PaymentStatus.FAILED) {

            if (order.getStatus() == OrderStatus.CANCELLED
                    && order.getPaymentStatus() == PaymentStatus.FAILED) {

                return;
            }

            throw new IllegalStateException(
                    "Payment and order statuses are inconsistent");
        }

        // 10. Only pending payments can fail
        if (payment.getStatus()
                != com.amazon_backend.payment.entity.PaymentStatus.PENDING) {

            throw new RuntimeException(
                    "Payment is not awaiting verification");
        }

        if (order.getStatus() != OrderStatus.PENDING
                || order.getPaymentStatus() != PaymentStatus.PENDING) {

            throw new RuntimeException(
                    "Order is not in a valid state for failed payment");
        }

        // 11. Restore reserved stock
        orderInventoryService.restoreReservedStock(
                order.getId(),
                reference,
                PaymentStatus.FAILED

        );

        // 12. Mark payment as failed
        payment.setStatus(
                com.amazon_backend.payment.entity.PaymentStatus.FAILED
        );

        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);
    }


    @Transactional
    public void confirmExpiredPayment(
            String email,
            String reference,
            Long amountInKobo,
            String currency,
            String customerEmail
    ) {

        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment reference is required");
        }

        Payment existingPayment = paymentRepository
                .findByReference(reference)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        Long orderId = existingPayment.getOrder().getId();

        // Always lock order before payment.
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        Payment payment = paymentRepository
                .findByReferenceForUpdate(reference)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        // Validate amount.
        long expectedAmount = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        if (amountInKobo == null || amountInKobo != expectedAmount) {
            throw new RuntimeException("Payment amount mismatch");
        }

        // Validate ownership.
        if (!order.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("Unauthorized payment verification");
        }

        // Validate reference.
        if (!reference.equals(payment.getReference())
                || !reference.equals(order.getPaymentReference())) {
            throw new RuntimeException("Payment reference mismatch");
        }

        // Validate currency.
        if (currency == null
                || !payment.getCurrency().equalsIgnoreCase(currency)) {
            throw new RuntimeException("Payment currency mismatch");
        }

        // Validate customer.
        if (customerEmail == null
                || !order.getUser().getEmail()
                .equalsIgnoreCase(customerEmail)) {
            throw new RuntimeException("Customer email mismatch");
        }

        // Handle duplicate expiration.
        if (payment.getStatus()
                == com.amazon_backend.payment.entity.PaymentStatus.FAILED) {

            if (order.getStatus() == OrderStatus.CANCELLED
                    && order.getPaymentStatus() == PaymentStatus.EXPIRED) {
                return;
            }

            throw new IllegalStateException(
                    "Payment and order statuses are inconsistent");
        }

        // Only pending payments can expire.
        if (payment.getStatus()
                != com.amazon_backend.payment.entity.PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment is not awaiting expiration");
        }

        if (order.getStatus() != OrderStatus.PENDING
                || order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Order is not in a valid state for expiration");
        }

        // Restore stock and expire the order.
        orderInventoryService.restoreReservedStock(
                order.getId(),
                reference,
                PaymentStatus.EXPIRED
        );

        // The payment entity has FAILED as its terminal unsuccessful
        // state; the order separately records EXPIRED.
        payment.setStatus(
                com.amazon_backend.payment.entity.PaymentStatus.FAILED);

        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);
    }

}
//the initializationLeaseToken is a security and concurrency control mechanism that helps us manage multiple payment initialization requests for the same order.
//
//Let me explain with an example from our marketplace.
//1. The problem it solves
//Imagine a customer places an order worth ₦150,000 and clicks Pay Now.
//
//The following happens:
//1. Our backend creates a payment record.
//2. It generates a unique initialization token.
//3. It sends a request to Paystack to initialize the transaction.
//4. Paystack returns the payment authorization URL.

//Paystack verification
//        |
//        v
//Validate amount
//        |
//        v
//Validate ownership
//        |
//        v
//Validate reference
//        |
//        v
//Validate currency
//        |
//        v
//Validate customer email
//        |
//        v
//Check duplicate verification
//        |
//        v
//Payment = SUCCESS
//        |
//        v
//Order PaymentStatus = PAID
//        |
//        v
//Order Status = CONFIRMED