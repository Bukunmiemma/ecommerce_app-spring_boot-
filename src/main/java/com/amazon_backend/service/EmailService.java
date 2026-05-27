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

    public void sendOtpEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset OTP");
        message.setText("Your OTP is: " + otp + "\nIt expires in 10 minutes.");

        mailSender.send(message);
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
////    message.setText("Click this link to reset your password:\n" + link);
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
