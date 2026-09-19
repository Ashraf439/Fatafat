package com.ashraf.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    /** Public URL of THIS backend, used in the email-verification link (was hardcoded to localhost:8080). */
    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    /** Local dev helper: also print every emailed link to the log so you can test flows without opening the inbox. */
    @Value("${app.dev.log-links:false}")
    private boolean logLinks;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String link = backendUrl + "/api/auth/verify?token=" + token;
        send(toEmail, "Verify your email",
                "Click the link to verify your account: " + link, link);
    }

    public void sendStaffInviteEmail(String toEmail, String restaurantName, String setPasswordUrl) {
        send(toEmail, "You've been added to " + restaurantName + " on Fatafat",
                "You have been added as a team member of " + restaurantName + " on Fatafat.\n\n"
                        + "Set your password to activate your account (link valid for 72 hours):\n"
                        + setPasswordUrl + "\n\n"
                        + "If you weren't expecting this, you can ignore this email.",
                setPasswordUrl);
    }

    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        send(toEmail, "Reset your Fatafat password",
                "We received a request to reset your password.\n\n"
                        + "Choose a new password here (link valid for 30 minutes):\n"
                        + resetUrl + "\n\n"
                        + "If you didn't ask for this, you can ignore this email.",
                resetUrl);
    }

    private void send(String toEmail, String subject, String text, String linkForDevLog) {
        if (logLinks) {
            log.info("[DEV] '{}' link for {}: {}", subject, toEmail, linkForDevLog);
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
        } catch (RuntimeException e) {
            if (!logLinks) throw e; // production: fail loudly
            log.warn("[DEV] Could not send email to {} ({}). Use the logged link above.", toEmail, e.getMessage());
        }
    }
}
