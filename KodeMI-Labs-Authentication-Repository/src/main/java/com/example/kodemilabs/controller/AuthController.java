package com.example.kodemilabs.controller;

import com.example.kodemilabs.config.RateLimitConfig;
import com.example.kodemilabs.dto.request.*;
import com.example.kodemilabs.exceptions.login.UserNotFoundException;
import com.example.kodemilabs.facade.AuthFacade;
import com.example.kodemilabs.model.ResetPassword;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.model.UserFootPrint;
import com.example.kodemilabs.repository.UserRepo;
import com.example.kodemilabs.service.UserFootPrintService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;

@RestController
@Validated
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final String TOO_MANY_REQUESTS_MSG = "Too many requests. Please try again later.";
    private static final String OTP_SENT_MSG = "OTP sent to email";
    private static final String OTP_RESENT_MSG = "Otp Resent Successfully";
    private static final String RESET_OTP_SENT_MSG = "Reset OTP sent to email";
    private static final String PASSWORD_RESET_SUCCESS_MSG = "Password reset successfully";
    private static final String USER_NOT_FOUND_MSG = "User not found";
    private static final String MESSAGE = "message";
    private static final String ERROR = "error";

    private final AuthFacade authFacade;
    private final UserFootPrintService userFootPrintService;
    private final RateLimitConfig.RateLimiter rateLimiter;
    private final UserRepo userRepo;

    public AuthController(AuthFacade authFacade,
                          UserFootPrintService userFootPrintService,
                          RateLimitConfig.RateLimiter rateLimiter,
                          UserRepo userRepo) {
        this.authFacade = authFacade;
        this.userFootPrintService = userFootPrintService;
        this.rateLimiter = rateLimiter;
        this.userRepo = userRepo;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@Valid @RequestBody LearnerRegistrationRequest request) {
        log.info("Learner Register Controller hit");
        authFacade.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(MESSAGE, OTP_SENT_MSG));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody UserLoginDTO request) {
        log.info("Login Controller hit");
        return ResponseEntity.ok(authFacade.login(request));
    }

    @PostMapping("/trainer/login")
    public ResponseEntity<Object> trainerLogin(@Valid @RequestBody UserLoginDTO request) {
        log.info("Login Controller hit");
        return ResponseEntity.ok(authFacade.trainerLogin(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<Object> verifyOtp(
            @RequestParam @Email @NotBlank String email,
            @RequestParam @Pattern(regexp = "^\\d{6}$") @NotBlank String otp,
            HttpServletRequest request) {

        if (!isAllowed(email, request)) return rateLimitExceededResponse();
        log.info("Learner verifyOtp Controller hit");
        return ResponseEntity.ok(authFacade.verifyOtpAndLogin(email, otp));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Object> refresh(@RequestBody Map<String, String> req) {
        log.info("refreshToken Controller hit");
        return ResponseEntity.ok(authFacade.refreshToken(req.get("refreshToken")));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Object> resendOtp(
            @Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest) {

        if (!isAllowed(request.getEmail(), httpRequest)) return rateLimitExceededResponse();
        log.info("Resend OTP requested");
        authFacade.resendOtp(request);
        return ResponseEntity.ok(Map.of(MESSAGE, OTP_RESENT_MSG));
    }

    @PostMapping("/footprint")
    public ResponseEntity<Object> captureFootprint(@RequestBody UserFootPrint footPrint) {
        userFootPrintService.save(footPrint);
        return ResponseEntity.ok(Map.of(MESSAGE, "Footprint captured"));
    }

    @PostMapping("/request-reset-password-otp")
    public ResponseEntity<Object> requestResetOtp(
            @RequestParam @Email @NotBlank String email,
            HttpServletRequest request) {

        if (!isAllowed(email, request)) return rateLimitExceededResponse();
        log.info("Password reset OTP requested");
        authFacade.requestResetOtp(email);
        return ResponseEntity.ok(Map.of(MESSAGE, RESET_OTP_SENT_MSG));
    }

    @PostMapping("/verify-reset-password-otp")
    public ResponseEntity<Object> verifyResetPasswordOtp(
            @RequestParam @Email @NotBlank String email,
            @RequestParam @Pattern(regexp = "^\\d{6}$") @NotBlank String otp) {

        try {
            String resetToken = authFacade.verifyResetOtp(email, otp);
            return ResponseEntity.ok(Map.of(
                    MESSAGE, "OTP verified successfully",
                    "resetToken", resetToken
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(ERROR, "Invalid or expired OTP"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Object> resetPassword(@Valid @RequestBody ResetPassword request) {
        try {
            authFacade.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of(MESSAGE, PASSWORD_RESET_SUCCESS_MSG));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(ERROR, e.getMessage()));
        }
    }

    @PostMapping("/register/trainer")
    public ResponseEntity<Object> registerTrainer(
            @Valid @RequestBody TrainerRegistrationRequest request) {
        log.info("Trainer Register Controller hit");
        authFacade.registerTrainer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                MESSAGE, "OTP sent to your email. Please verify."
        ));
    }

    @PostMapping("/verify/trainer")
    public ResponseEntity<Object> verifyTrainerOtp(
            @RequestParam @Email @NotBlank String email,
            @RequestParam @Pattern(regexp = "^\\d{6}$") @NotBlank String otp,
            HttpServletRequest request) {

        if (!isAllowed(email, request)) return rateLimitExceededResponse();
        log.info("Trainer OTP verify Controller hit");
        return ResponseEntity.ok(authFacade.verifyTrainerOtp(email, otp));
    }

    @PostMapping("/internal/trainer/submitted/{userId}")
    @PreAuthorize("hasAnyAuthority('INTERNAL', 'TRAINER')")
    public ResponseEntity<Object> markTrainerSubmitted(@PathVariable String userId) {
        log.info("Trainer profile submitted, marking APPROVAL_PENDING: {}", userId);
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            throw new UserNotFoundException(USER_NOT_FOUND_MSG);
        }
        user.setStatus("APPROVAL_PENDING");
        userRepo.save(user);
        return ResponseEntity.ok(Map.of(MESSAGE, "Profile submitted for admin review"));
    }

    @PutMapping("/internal/trainer/review/{userId}")
    @PreAuthorize("hasAnyAuthority('INTERNAL', 'USER_ADMIN')")
    public ResponseEntity<Object> reviewTrainer(
            @PathVariable String userId,
            @RequestBody Map<String, String> request,
            org.springframework.security.core.Authentication authentication) {
        
        String action = request.get("action");
        if (action == null || action.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, "Action is required. Use 'VERIFY' or 'REJECT'."));
        }
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            throw new UserNotFoundException(USER_NOT_FOUND_MSG);
        }

        String adminId = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            adminId = jwt.getClaimAsString("userId");
        }

        action = action.toUpperCase().trim();
        if ("VERIFY".equals(action)) {
            log.info("Admin approved trainer: {}", userId);
            user.setStatus("ACTIVE");
            user.setActive(true);
            user.setVerifierId(adminId);
            userRepo.save(user);
            return ResponseEntity.ok(Map.of(MESSAGE, "Trainer activated successfully"));
        } else if ("REJECT".equals(action)) {
            log.info("Admin rejected trainer: {}", userId);
            user.setStatus("REJECTED");
            user.setVerifierId(adminId);
            userRepo.save(user);
            return ResponseEntity.ok(Map.of(MESSAGE, "Trainer rejected"));
        } else {
            return ResponseEntity.badRequest().body(Map.of(ERROR, "Invalid action. Use 'VERIFY' or 'REJECT'."));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Object> checkStatus(
            @RequestParam @Email @NotBlank String email) {
        log.info("Status check for: {}", email);
        User user = userRepo.getUserByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(ERROR, USER_NOT_FOUND_MSG));
        }
        return ResponseEntity.ok(Map.of(
                "role",   user.getRole().name(),
                "status", user.getStatus(),
                "active", user.isActive()
        ));
    }

    @GetMapping("/pending/trainer")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    public ResponseEntity<List<String>> getPendingTrainer(){
        List<String> response = new ArrayList<>();
        for(User user : userRepo.getPendingTrainers()){
            response.add(user.getUserId());
        }
        return ResponseEntity.ok(response);
    }

    private boolean isAllowed(String email, HttpServletRequest request) {
        String identifier = email + ":" + getClientIP(request);
        return rateLimiter.allowRequest(identifier);
    }

    private ResponseEntity<Object> rateLimitExceededResponse() {
        log.warn("Rate limit exceeded");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(ERROR, TOO_MANY_REQUESTS_MSG));
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        return (xfHeader == null) ? request.getRemoteAddr() : xfHeader.split(",")[0];
    }
}