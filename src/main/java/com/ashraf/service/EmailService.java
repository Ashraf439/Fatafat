package com.ashraf.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private  JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void  sendVerificationEmail(String toEmail, String token) {
        SimpleMailMessage message =  new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Verify your email: ");
        message.setText("Click the link to verify your account: "
                + "http://localhost:8080/api/auth/verify?token=" + token);
        mailSender.send(message);
    }
}
