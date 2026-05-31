package com.amazon_backend.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private JavaMailSender mailSender;

    public  EmailService(JavaMailSender mailSender){
        this.mailSender = mailSender;
    }

    public void sendResetOtp(String email, String otp) {

        try{
            System.out.println("STARTING EMAIL SEND");
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Password Reset OTP");
            message.setText("Your OTP is: " + otp + "\n It expires in 10 minutes.");

            mailSender.send(message);
            System.out.println("OTP EMAIL SENT SUCCESSFULLY");}
        catch (Exception e){
            System.out.println("EMAIL ERROR: " + e.getMessage());

            e.printStackTrace();
        }


    }
//        public void sendResetEmail(String email, String link) {
//
//
//try{
//    System.out.println("Sending email to: " + email);
//    System.out.println("Reset Link: " + link);
//    SimpleMailMessage message = new SimpleMailMessage();
//    message.setTo(email);
//    message.setSubject("Password Reset Request");
//  message.setText("Click this link to reset your password:\n" + link);
//    message.setText(
//            "Copy and paste this link into your browser:\n\n"
//                    + link
//    );
//    mailSender.send(message);
//    System.out.println("EMAIL SENT SUCCESSFULLY");
//}
//        catch (Exception e) {
//        System.out.println("EMAIL FAILED: " + e.getMessage());
//        throw e;
//    }
//
//        }


}
