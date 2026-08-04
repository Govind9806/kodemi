package com.example.kodemilabs.service;
import com.example.kodemilabs.dto.request.ResendOtpRequest;
import com.example.kodemilabs.exceptions.login.AccountLockedException;
import com.example.kodemilabs.exceptions.login.TooManyRequestsException;
import com.example.kodemilabs.exceptions.login.UserNotFoundException;
import com.example.kodemilabs.exceptions.otp.InvalidTokenException;
import com.example.kodemilabs.exceptions.otp.OtpAlreadyUsedException;
import com.example.kodemilabs.exceptions.otp.TokenExpiredException;
import com.example.kodemilabs.model.OTP;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.repository.OTPRepo;
import com.example.kodemilabs.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
public class OTPService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_MAX_VALUE = 1000000;
    private static final long OTP_EXPIRY_MINUTES = 5;

    private static final String USER_NOT_FOUND = "User not found";
    private static final String OTP_ALREADY_USED = "OTP already used";
    private static final String OTP_EXPIRED = "OTP expired";
    private static final String INVALID_OTP = "Invalid OTP";

    private final SecureRandom secureRandom = new SecureRandom();
    private final EmailService emailService;
    private final OTPRepo otpRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired
    public OTPService(EmailService emailService, OTPRepo otpRepo, UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.emailService = emailService;
        this.otpRepo = otpRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder != null ? passwordEncoder : new BCryptPasswordEncoder();
    }

    public OTPService(EmailService emailService, OTPRepo otpRepo, UserRepo userRepo) {
        this(emailService, otpRepo, userRepo, new BCryptPasswordEncoder());
    }

    public void generateOtp(String userId) {
        User user = getUserOrThrow(userId);
        createAndSendOtp(user);
    }

    public void generateResetPasswordOtp(String email) {
        log.info("Password reset OTP requested");
        User user = getUserByEmailOrThrow(email);
        deleteExistingOtp(user.getUserId());
        createAndSendOtp(user);
        log.info("Password reset OTP sent successfully");
    }

    private User getUserOrThrow(String userId) {
        if (userId == null) throw new IllegalArgumentException("userId cannot be null");
        User user = userRepo.getUserById(userId);
        if (user == null) throw new UserNotFoundException(USER_NOT_FOUND);
        return user;
    }

    private User getUserByEmailOrThrow(String email) {
        User user = userRepo.getUserByEmail(email);
        if (user == null) throw new UserNotFoundException(USER_NOT_FOUND);
        return user;
    }

    private void createAndSendOtp(User user) {
        String rawOtp = generateSecureOtp();
        String hashedOtp = passwordEncoder.encode(rawOtp);

        OTP otp = new OTP();
        otp.setUserId(user.getUserId());
        otp.setOtpCode(hashedOtp);
        otp.setExpireAt(calculateExpiryTime());
        otp.setEnable(true);

        otpRepo.save(otp);
        log.info("OTP stored successfully");

        emailService.sendOtpEmail(user.getEmail(), rawOtp);
        log.info("OTP email sent successfully");
    }

    private String generateSecureOtp() {
        return String.format("%0" + OTP_LENGTH + "d", secureRandom.nextInt(OTP_MAX_VALUE));
    }

    private long calculateExpiryTime() {
        return System.currentTimeMillis() + (OTP_EXPIRY_MINUTES * 60L * 1000L);
    }

    private void deleteExistingOtp(String userId) {
        OTP existingOtp = otpRepo.getOtpData(userId);
        if (existingOtp != null) {
            otpRepo.delete(existingOtp);
            log.info("Deleted existing OTP");
        }
    }

    public void verifyOtp(String email, String otpCode) {
        User user = getUserByEmailOrThrow(email);
        verifyOtpInternal(user, otpCode);
        log.info("OTP verified successfully");
    }

    public void verifyResetPasswordOtp(String email, String otpCode) {
        User user = getUserByEmailOrThrow(email);
        verifyOtpInternal(user, otpCode);
        log.info("Reset password OTP verified successfully");
    }

    private void verifyOtpInternal(User user, String otpCode) {
        if (user.getLockedUntil() != null && user.getLockedUntil() > System.currentTimeMillis()) {
            throw new AccountLockedException("Account is locked due to too many failed attempts. Try again later.");
        }

        OTP otp = otpRepo.getOtpData(user.getUserId());

        if (otp == null) throw new InvalidTokenException(INVALID_OTP);
        if (!otp.isEnable()) throw new OtpAlreadyUsedException(OTP_ALREADY_USED);
        if (System.currentTimeMillis() > otp.getExpireAt()) {
            otpRepo.delete(otp);
            throw new TokenExpiredException(OTP_EXPIRED);
        }
        
        if (!passwordEncoder.matches(otpCode, otp.getOtpCode())) {
            int failedAttempts = otp.getFailedAttempts() == null ? 0 : otp.getFailedAttempts();
            failedAttempts++;
            otp.setFailedAttempts(failedAttempts);
            
            if (failedAttempts >= 5) {
                user.setLockedUntil(System.currentTimeMillis() + (15 * 60 * 1000L));
                userRepo.save(user);
                otp.setEnable(false);
                otpRepo.save(otp);
                log.warn("Account locked for 15 minutes due to 5 failed OTP attempts: {}", maskEmailForLog(user.getEmail()));
                throw new AccountLockedException("Too many failed attempts. Account locked for 15 minutes.");
            }
            otpRepo.save(otp);
            throw new InvalidTokenException(INVALID_OTP);
        }

        otp.setEnable(false);
        otpRepo.save(otp);
    }
    
    private String maskEmailForLog(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        return parts[0].substring(0, Math.min(2, parts[0].length())) + "***@" + parts[1];
    }

    public void resendOtp(ResendOtpRequest request) {
        User user = getUserByEmailOrThrow(request.getEmail());
        
        OTP existingOtp = otpRepo.getOtpData(user.getUserId());
        if (existingOtp != null) {
            long createdAt = existingOtp.getExpireAt() - (OTP_EXPIRY_MINUTES * 60L * 1000L);
            if ((System.currentTimeMillis() - createdAt) < (60 * 1000L)) {
                log.warn("Rate limit exceeded for resending OTP: {}", maskEmailForLog(user.getEmail()));
                throw new TooManyRequestsException("Please wait 60 seconds before requesting another OTP.");
            }
            otpRepo.delete(existingOtp);
            log.info("Deleted existing OTP for resend");
        }
        
        generateOtp(user.getUserId());
        log.info("Resent OTP successfully");
    }
}
