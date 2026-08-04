package com.example.kodemilabs.service;

import com.example.kodemilabs.exceptions.login.UserNotFoundException;
import com.example.kodemilabs.exceptions.otp.TokenExpiredException;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.repository.UserRepo;
import com.example.kodemilabs.util.PasswordValidator;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PasswordResetService {

    private final OTPService otpService;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    final Map<String, String> resetTokens = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    @org.springframework.beans.factory.annotation.Autowired
    public PasswordResetService(OTPService otpService, UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.otpService = otpService;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder != null ? passwordEncoder : new BCryptPasswordEncoder();
    }

    public PasswordResetService(OTPService otpService, UserRepo userRepo) {
        this(otpService, userRepo, new BCryptPasswordEncoder());
    }

    public void requestResetPasswordOtp(String email) {
        try {
            otpService.generateResetPasswordOtp(email);
        } catch (UserNotFoundException e) {
            log.warn("Password reset requested for non-existent email. Swallowing request to prevent enumeration: {}", email);
        }
    }

    public String verifyResetPasswordOtp(String email, String otp) {
        otpService.verifyResetPasswordOtp(email, otp);

        String token = UUID.randomUUID().toString();
        resetTokens.put(token, email);

        scheduler.schedule(() -> {
            resetTokens.remove(token);
            log.info("Reset token expired and removed for email: {}", email);
        }, 15, TimeUnit.MINUTES);

        log.info("OTP verified for {}. Reset token issued: {}", email, token);
        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {

        String email = resetTokens.get(token);
        if (email == null) {
            throw new TokenExpiredException("Invalid or expired reset token");
        }

        User user = userRepo.getUserByEmail(email);
        if (user == null) {
            throw new UserNotFoundException("User not found with email: " + email);
        }

        PasswordValidator.validate(newPassword);

        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashedPassword);
        userRepo.save(user);

        resetTokens.remove(token);

        log.info("Password reset successfully for email: {}", email);
    }

    @PreDestroy
    public void shutdownScheduler() {
        log.info("Shutting down PasswordResetService scheduler...");
        scheduler.shutdown();
    }
}