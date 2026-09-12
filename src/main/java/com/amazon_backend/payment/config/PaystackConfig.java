package com.amazon_backend.payment.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

//This tells Spring: This class contains configuration that Spring should manage.
@Configuration
public class PaystackConfig {

    //paystack.secret.key=${PAYSTACK_SECRET_KEY}
    //Spring gets the value from Render's environment variable.
    @Value("${paystack.secret.key}")
    private String secretKey;

    @Value("${paystack.base.url}")
    private String baseUrl;

    //RestClient is Spring's HTTP client that we'll use to communicate with Paystack.
    //For example, later our backend will effectively make:
    //POST https://api.paystack.co/transaction/initialize
    //with:
    //Authorization: Bearer sk_test_...
    //The RestClient configuration above means we don't have to manually add those headers every time.
    @Bean
    public RestClient paystackRestClient() {
            return RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + secretKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        }
    }

