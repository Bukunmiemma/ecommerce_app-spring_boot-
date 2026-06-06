package com.amazon_backend.service;

import com.resend.services.emails.model.CreateEmailOptions;

import org.springframework.stereotype.Service;


import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    public void sendResetOtp(String email, String otp) {

        try {

            System.out.println("STARTING RESEND EMAIL SEND");

            Resend resend = new Resend(apiKey);

            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from("onboarding@resend.dev")
                    .to(email)
                    .subject("Password Reset OTP")
                    .html(
                            "<h2>Password Reset OTP</h2>" +
                                    "<p>Your OTP is: <strong>" + otp + "</strong></p>" +
                                    "<p>It expires in 10 minutes.</p>"
                    )
                    .build();

            resend.emails().send(request);

            System.out.println("OTP EMAIL SENT SUCCESSFULLY");

        } catch (Exception e) {

            System.out.println("EMAIL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}