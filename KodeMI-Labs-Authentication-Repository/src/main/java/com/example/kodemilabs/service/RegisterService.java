package com.example.kodemilabs.service;

import com.example.kodemilabs.dto.request.LearnerRegistrationRequest;
import com.example.kodemilabs.dto.request.TrainerRegistrationRequest;
import com.example.kodemilabs.dto.request.UserDTO;
import com.example.kodemilabs.dto.response.LoginResponse;
import com.example.kodemilabs.enums.Role;
import com.example.kodemilabs.exceptions.login.RegistrationFailedException;
import com.example.kodemilabs.exceptions.login.UserNotFoundException;
import com.example.kodemilabs.exceptions.registration.EmailAlreadyExistsException;
import com.example.kodemilabs.feign.PaymentClient;
import com.example.kodemilabs.feign.UserClient;
import com.example.kodemilabs.mapper.UserMapper;
import com.example.kodemilabs.model.RefreshToken;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.repository.RefreshTokenRepo;
import com.example.kodemilabs.repository.UserRepo;
import com.example.kodemilabs.util.PasswordValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
public class RegisterService {

    private static final String REGISTRATION_FAILED_MSG = "Registration failed";
    private static final String USER_NOT_FOUND_MSG = "User not found after OTP verification";

    private final UserRepo userRepo;
    private final OTPService otpService;
    private final JwtService jwtService;
    private final RefreshTokenRepo refreshTokenRepo;
    private final UserClient userClient;
    private final PaymentClient paymentClient;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired
    public RegisterService(UserRepo userRepo,
            OTPService otpService,
            JwtService jwtService,
            RefreshTokenRepo refreshTokenRepo,
            UserClient userClient,
            PaymentClient paymentClient,
            PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.refreshTokenRepo = refreshTokenRepo;
        this.userClient = userClient;
        this.paymentClient = paymentClient;
        this.passwordEncoder = passwordEncoder != null ? passwordEncoder : new BCryptPasswordEncoder();
    }

    public RegisterService(UserRepo userRepo,
            OTPService otpService,
            JwtService jwtService,
            RefreshTokenRepo refreshTokenRepo,
            UserClient userClient,
            PaymentClient paymentClient) {
        this(userRepo, otpService, jwtService, refreshTokenRepo, userClient, paymentClient, new BCryptPasswordEncoder());
    }

    @Transactional
    public void register(LearnerRegistrationRequest request) {
        log.info("Learner register attempt: {}", request.getEmail());
        PasswordValidator.validate(request.getPasswordHash());

        if (userRepo.getUserByEmail(request.getEmail()) != null) {
            log.warn("Registration requested for existing email. Throwing exception: {}", request.getEmail());
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = buildNewLearner(request);
        try {
            userRepo.save(user);
            log.info("Learner saved: {}", user.getEmail());
            otpService.generateOtp(user.getUserId());
            log.info("OTP sent to learner: {}", user.getEmail());
        } catch (RuntimeException e) {
            log.error(REGISTRATION_FAILED_MSG, e);
            throw new RegistrationFailedException(REGISTRATION_FAILED_MSG);
        }
    }

    @Transactional
    public LoginResponse verifyOtpAndLogin(String email, String otpCode) {
        log.info("Learner OTP verification: {}", email);

        User user = userRepo.getUserByEmail(email);
        if (user == null)
            throw new UserNotFoundException(USER_NOT_FOUND_MSG);

        if (!Boolean.TRUE.equals(user.isVerified())) {
            otpService.verifyOtp(email, otpCode);
            user.setVerified(true);
            user.setStatus("ACTIVE");
            user.setLastLogin(Instant.now().getEpochSecond());
            userRepo.save(user);
        } else {
            log.info("User {} is already verified. Skipping OTP verification and retrying User-Service sync.", email);
        }

        String jwt = jwtService.generateToken(
                user.getUserId(), user.getEmail(), user.getRole().name(), user.getUsername());
        String refreshToken = manageRefreshToken(user.getEmail());
        log.info(jwt);

        UserDTO userDTO = UserMapper.toDTO(user);
        userClient.addLearner(userDTO);

        try {
            paymentClient.createWallet(jwt);
        } catch (Exception e) {
            log.error("Failed to create wallet for learner: {}. Error: {}", email, e.getMessage());
        }

        log.info("Learner verified and pushed to user-service: {}", email);
        return new LoginResponse(jwt, refreshToken, user.getRole().name());
    }

    @Transactional
    public void registerTrainer(TrainerRegistrationRequest request) {
        log.info("Trainer register attempt: {}", request.getEmail());
        PasswordValidator.validate(request.getPassword());

        if (userRepo.getUserByEmail(request.getEmail()) != null) {
            log.warn("Trainer registration requested for existing email. Throwing exception: {}", request.getEmail());
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.TRAINER);
        user.setActive(false);
        user.setVerified(false);
        user.setStatus("PENDING_EMAIL");
        user.setLastLogin(null);

        try {
            userRepo.save(user);
            log.info("Trainer saved in user table: {}", user.getEmail());
            otpService.generateOtp(user.getUserId());
            log.info("OTP sent to trainer: {}", user.getEmail());
        } catch (RuntimeException e) {
            log.error("Trainer registration failed", e);
            throw new RegistrationFailedException(REGISTRATION_FAILED_MSG);
        }
    }

    @Transactional
    public LoginResponse verifyTrainerOtp(String email, String otpCode) {
        log.info("Trainer OTP verification: {}", email);

        User user = userRepo.getUserByEmail(email);
        if (user == null)
            throw new UserNotFoundException(USER_NOT_FOUND_MSG);

        if (!Boolean.TRUE.equals(user.isVerified())) {
            otpService.verifyOtp(email, otpCode);
            user.setVerified(true);
            user.setStatus("PROFILE_PENDING");
            user.setLastLogin(Instant.now().getEpochSecond());
            userRepo.save(user);
            log.info("Trainer OTP verified, status=PROFILE_PENDING: {}", email);
        } else {
            log.info("Trainer {} is already verified. Skipping OTP verification.", email);
        }

        String jwt = jwtService.generateToken(
                user.getUserId(), user.getEmail(), user.getRole().name(), user.getUsername());
        String refreshToken = manageRefreshToken(user.getEmail());
        log.info(jwt);

        try {
            paymentClient.createWallet("Bearer " + jwt);
        } catch (Exception e) {
            log.error("Failed to create wallet for trainer: {}. Error: {}", email, e.getMessage());
        }

        return new LoginResponse(jwt, refreshToken, user.getRole().name());
    }

    private User buildNewLearner(LearnerRegistrationRequest request) {
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPasswordHash()));
        user.setActive(true);
        user.setVerified(false);
        user.setRole(Role.LEARNER);
        user.setStatus("ACTIVE");
        user.setLastLogin(null);
        return user;
    }

    private String manageRefreshToken(String email) {
        RefreshToken existing = refreshTokenRepo.findByEmail(email);
        if (existing != null) {
            log.info("Deleting old refresh token: {}", email);
            refreshTokenRepo.delete(existing.getToken());
        }
        String refreshToken = UUID.randomUUID().toString();
        RefreshToken rt = new RefreshToken();
        rt.setToken(refreshToken);
        rt.setEmail(email);
        rt.setExpiry(Instant.now().plus(7, ChronoUnit.DAYS).getEpochSecond());
        refreshTokenRepo.save(rt);
        log.info("New refresh token created: {}", email);
        return refreshToken;
    }
}