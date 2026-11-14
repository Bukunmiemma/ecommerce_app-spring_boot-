package com.amazon_backend.auth.service;

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

    // NEW EMAIL VERIFICATION EMAIL
    public void sendEmailVerificationOtp(String email, String otp) {

        try {

            Resend resend = new Resend(apiKey);

            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from("onboarding@resend.dev")
                    .to(email)
                    .subject("Verify Your Email Address")
                    .html(
                            "<div style='font-family:Arial,sans-serif;'>" +
                                    "<h2>Welcome to our marketplace!</h2>" +
                                    "<p>Thank you for creating an account.</p>" +
                                    "<p>Use the verification code below to verify your email:</p>" +
                                    "<h1 style='letter-spacing:8px;'>" + otp + "</h1>" +
                                    "<p>This code expires in 5 minutes.</p>" +
                                    "<p>If you did not create this account, you can ignore this email.</p>" +
                                    "</div>"
                    )
                    .build();

            resend.emails().send(request);

            System.out.println("EMAIL VERIFICATION EMAIL SENT");

        } catch (Exception e) {

            System.out.println("EMAIL VERIFICATION ERROR: " + e.getMessage());

            throw new IllegalStateException(
                    "Unable to send verification email",
                    e
            );
        }
    }
}