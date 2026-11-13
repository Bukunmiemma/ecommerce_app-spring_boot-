package com.amazon_backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

//Paystack response to our backend
public class PaystackInitializeResponse {
    private boolean status;
    private String message;
    private Data data;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }



    public static class Data {

        @JsonProperty("authorization_url")
        private String authorizationUrl;

        @JsonProperty("access_code")
        private String accessCode;

        private String reference;

        public String getAuthorizationUrl() {
            return authorizationUrl;
        }

        public void setAuthorizationUrl(String authorizationUrl) {
            this.authorizationUrl = authorizationUrl;
        }

        public String getAccessCode() {
            return accessCode;
        }

        public void setAccessCode(String accessCode) {
            this.accessCode = accessCode;
        }

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }
    }




    }
//                    OUR SYSTEM
//                         │
//Flutter ──► PaymentInitializeRequest
//                         │
//                         ▼
//                  PaymentService
//                         │
//                         ▼
//             PaystackInitializeRequest
//                         │
//                         ▼
//                      Paystack
//                         │
//                         ▼
//             PaystackInitializeResponse
//                         │
//                         ▼
//                  PaymentService
//                         │
//                         ▼
//             PaymentInitializeResponse
//                         │
//                         ▼
//
//                     Flutter

//File	Purpose
//PaystackConfig.java	Configures Paystack HTTP communication
//PaymentController.java	Exposes payment endpoints
//PaymentService.java	Payment service interface
//PaymentServiceImpl.java	Payment initialization logic
//PaymentInitializeRequest.java	Request from Flutter
//PaymentInitializeResponse.java	Response to Flutter
//PaystackInitializeRequest.java	Request sent to Paystack
//PaystackInitializeResponse.java	Response received from Paystack
//Payment.java	Stores payment records
//PaymentRepository.java	Payment database operations