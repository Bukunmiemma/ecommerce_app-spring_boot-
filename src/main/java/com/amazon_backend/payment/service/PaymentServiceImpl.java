package com.amazon_backend.payment.service;
import com.amazon_backend.payment.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentPreparationService paymentPreparationService;
    private final RestClient paystackRestClient;

    public PaymentServiceImpl(
            PaymentPreparationService paymentPreparationService,
            RestClient paystackRestClient
    ) {
        this.paymentPreparationService = paymentPreparationService;
        this.paystackRestClient = paystackRestClient;
    }

    @Override
    public PaymentInitializeResponse initializePayment(
            String email,
            PaymentInitializeRequest request
    ) {

        // 1. Prepare payment inside a database transaction
        PaymentPreparationResult preparation =
                paymentPreparationService.preparePayment(
                        email,
                        request.getOrderId()
                );

        // 2. If authorization details already exist, reuse them
        if (preparation.authorizationUrl() != null
                && preparation.accessCode() != null) {

            return buildResponse(
                    preparation.authorizationUrl(),
                    preparation.accessCode(),
                    preparation.reference()
            );
        }

        // 3. Convert Naira to Kobo
        long amountInKobo = preparation.amount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        // 4. Build Paystack request
        PaystackInitializeRequest paystackRequest =
                new PaystackInitializeRequest();

        paystackRequest.setAmount(amountInKobo);
        paystackRequest.setEmail(preparation.email());
        paystackRequest.setReference(preparation.reference());
        paystackRequest.setCurrency(preparation.currency());

        // 5. Communicate with Paystack
        PaystackInitializeResponse paystackResponse =
                paystackRestClient
                        .post()
                        .uri("/transaction/initialize")
                        .body(paystackRequest)
                        .retrieve()
                        .body(PaystackInitializeResponse.class);

        // 6. Validate Paystack response
        if (paystackResponse == null
                || !paystackResponse.isStatus()
                || paystackResponse.getData() == null) {

            throw new RuntimeException(
                    "Paystack payment initialization failed"
            );
        }

        var data = paystackResponse.getData();

        if (data.getAuthorizationUrl() == null
                || data.getAuthorizationUrl().isBlank()
                || data.getAccessCode() == null
                || data.getAccessCode().isBlank()
                || data.getReference() == null
                || !preparation.reference().equals(data.getReference())) {

            throw new RuntimeException(
                    "Invalid response received from Paystack"
            );
        }

        // 7. Save authorization details in a separate transaction
        //Why is this important?
        //When Paystack responds, our backend must prove that the
        // request saving the authorization details still owns the
        // initialization lease. This token provides that check

        paymentPreparationService.saveAuthorizationDetails(
                preparation.reference(),
                preparation.initializationLeaseToken(),
                data.getAuthorizationUrl(),
                data.getAccessCode()

                );

        // 8. Return payment details to Flutter
        return buildResponse(
                data.getAuthorizationUrl(),
                data.getAccessCode(),
                data.getReference()
        );
    }


    @Override
    public void verifyPayment(String email, String reference) {

        // 1. Validate payment reference
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment reference is required");
        }

        // 2. Ask Paystack to verify the transaction
        PaystackVerifyResponse response = paystackRestClient
                .get()
                .uri("/transaction/verify/{reference}", reference)
                .retrieve()
                .body(PaystackVerifyResponse.class);

        // 3. Validate Paystack response
        if (response == null
                || !response.isStatus()
                || response.getData() == null) {

            throw new RuntimeException(
                    "Unable to verify payment with Paystack");
        }

        PaystackVerifyResponse.Data data = response.getData();

        // 4. Validate transaction reference
        if (!reference.equals(data.getReference())) {
            throw new RuntimeException(
                    "Paystack transaction reference mismatch");
        }

        // 5. Validate customer information
        if (data.getCustomer() == null
                || data.getCustomer().getEmail() == null
                || data.getCustomer().getEmail().isBlank()) {

            throw new RuntimeException(
                    "Paystack customer information is missing");
        }

        // 6. Read Paystack transaction status
        String paymentStatus = data.getStatus();

        if (paymentStatus == null || paymentStatus.isBlank()) {
            throw new RuntimeException(
                    "Paystack transaction status is missing");
        }

        // 7. Handle successful payment
        if ("success".equalsIgnoreCase(paymentStatus)) {

            paymentPreparationService.confirmSuccessfulPayment(
                    email,
                    data.getReference(),
                    data.getAmount(),
                    data.getCurrency(),
                    data.getCustomer().getEmail()
            );

            return;
        }

        // 8. Handle genuinely failed payment
        if ("failed".equalsIgnoreCase(paymentStatus)) {

            paymentPreparationService.confirmFailedPayment(
                    email,
                    data.getReference(),
                    data.getAmount(),
                    data.getCurrency(),
                    data.getCustomer().getEmail()
            );

            return;
        }

        // 9. Do not cancel orders for pending or unknown statuses
        if ("pending".equalsIgnoreCase(paymentStatus)) {

            throw new RuntimeException(
                    "Payment is still pending with Paystack");
        }

        throw new RuntimeException(
                "Payment cannot be processed. Paystack status: "
                        + paymentStatus);

        //We are not restoring stock for pending transactions.
        //A pending transaction isn't necessarily a failed transaction.
        //Also, Paystack's abandoned status is not automatically treated as a confirmed failure.
        // We'll handle expiration and abandoned payments separately.
    }


    public void verifyPaymentForExpiration(String reference) {

        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment reference is required");
        }

        // Ask Paystack for the actual transaction status.
        PaystackVerifyResponse response = paystackRestClient
                .get()
                .uri("/transaction/verify/{reference}", reference)
                .retrieve()
                .body(PaystackVerifyResponse.class);

        if (response == null
                || !response.isStatus()
                || response.getData() == null) {

            throw new RuntimeException(
                    "Unable to verify payment with Paystack");
        }

        PaystackVerifyResponse.Data data = response.getData();

        if (!reference.equals(data.getReference())) {
            throw new RuntimeException(
                    "Payment reference mismatch");
        }

        if (data.getCustomer() == null
                || data.getCustomer().getEmail() == null) {

            throw new RuntimeException(
                    "Paystack customer information is missing");
        }

        String status = data.getStatus();

        if (status == null || status.isBlank()) {
            throw new RuntimeException(
                    "Paystack transaction status is missing");
        }

        // Payment succeeded.
        if ("success".equalsIgnoreCase(status)) {

            paymentPreparationService.confirmSuccessfulPayment(
                    data.getCustomer().getEmail(),
                    data.getReference(),
                    data.getAmount(),
                    data.getCurrency(),
                    data.getCustomer().getEmail()
            );

            return;
        }

        // Payment genuinely failed.
        if ("failed".equalsIgnoreCase(status)) {

            paymentPreparationService.confirmFailedPayment(
                    data.getCustomer().getEmail(),
                    data.getReference(),
                    data.getAmount(),
                    data.getCurrency(),
                    data.getCustomer().getEmail()
            );

            return;
        }

        // Customer abandoned the transaction.
        if ("abandoned".equalsIgnoreCase(status)) {

            paymentPreparationService.confirmExpiredPayment(
                    data.getCustomer().getEmail(),
                    data.getReference(),
                    data.getAmount(),
                    data.getCurrency(),
                    data.getCustomer().getEmail()
            );

            return;
        }

        // Pending, processing, ongoing or unknown:
        // Do not release inventory.
    }

    private PaymentInitializeResponse buildResponse(
            String authorizationUrl,
            String accessCode,
            String reference
    ) {

        PaymentInitializeResponse response =
                new PaymentInitializeResponse();

        response.setAuthorizationUrl(authorizationUrl);
        response.setAccessCode(accessCode);
        response.setReference(reference);

        return response;
    }
}
//Your PaymentPreparationService is saving the payment reference and updating the order.
//Your PaymentServiceImpl is receiving the authorization URL and access code from Paystack

//Our payment initialization flow will be:
//1. Prepare payment and reserve initialization ownership.
//2. Contact Paystack.
//3. Validate Paystack's response.
//4. Verify the initialization token.
//5. Save the authorization URL and access code.
//6. Return the payment details to Flutter.