package com.example.kodemilabs.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email must not be null or empty");
        }
        if (otp == null || otp.isBlank()) {
            throw new IllegalArgumentException("OTP must not be null or empty");
        }

        log.info("Sending OTP email");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("KodeMI - OTP Verification");
        message.setText(
                "Your OTP for KodeMI registration is: " + otp +
                        "\n\nThis OTP is valid for 5 minutes." +
                        "\n\nDo not share this OTP with anyone."
        );

        try {
            mailSender.send(message);
            log.info("OTP email sent successfully");
        } catch (Exception e) {
            log.error("Failed to send OTP email", e);
            throw e;
        }
    }
}