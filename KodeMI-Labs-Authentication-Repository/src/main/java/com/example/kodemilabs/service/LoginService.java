package com.example.kodemilabs.service;

import com.example.kodemilabs.dto.request.AdminLoginRequest;
import com.example.kodemilabs.dto.request.UserLoginDTO;
import com.example.kodemilabs.dto.response.LoginResponse;
import com.example.kodemilabs.dto.response.OtpPendingResponse;
import com.example.kodemilabs.enums.Role;
import com.example.kodemilabs.exceptions.jwt.AccessDeniedException;
import com.example.kodemilabs.exceptions.login.InvalidCredentialsException;
import com.example.kodemilabs.model.RefreshToken;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.repository.RefreshTokenRepo;
import com.example.kodemilabs.repository.UserRepo;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
public class LoginService {

    private static final String AUTH_FAILED = "Authentication failed";
    private static final String UNAUTHORIZED_LOGIN = "UnAuthorized Login.";

    private final OTPService otpService;
    private final UserRepo userRepo;
    private final JwtService jwtService;
    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired
    public LoginService(OTPService otpService,
                        UserRepo userRepo,
                        JwtService jwtService,
                        RefreshTokenRepo refreshTokenRepo,
                        PasswordEncoder passwordEncoder) {
        this.otpService = otpService;
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder != null ? passwordEncoder : new BCryptPasswordEncoder();
    }

    public LoginService(OTPService otpService,
                        UserRepo userRepo,
                        JwtService jwtService,
                        RefreshTokenRepo refreshTokenRepo) {
        this(otpService, userRepo, jwtService, refreshTokenRepo, new BCryptPasswordEncoder());
    }

    @Transactional
    public Object login(@Valid UserLoginDTO userLoginDTO) {

        String email = userLoginDTO.getEmail();
        User user = userRepo.getUserByEmail(email);

        if (user == null) {
            log.warn("Login failed – not found: {}", email);
            throw new InvalidCredentialsException(AUTH_FAILED);
        }

        if (!passwordEncoder.matches(userLoginDTO.getPassword(), user.getPasswordHash())) {
            log.warn("Wrong password for: {}", email);
            throw new InvalidCredentialsException(AUTH_FAILED);
        }

        if (!user.isVerified()) {
            log.info("Not verified, resending OTP: {}", email);
            otpService.generateOtp(user.getUserId());
            return new OtpPendingResponse("OTP sent to email", true);
        }

        if (user.getRole() == Role.TRAINER || user.getRole() == Role.SUPER_ADMIN) {
            log.info("Unauthorized login attempt: {}", user.getUsername());
            throw new AccessDeniedException(UNAUTHORIZED_LOGIN);
        }

        log.info("Login successful: {}", email);

        String jwt = jwtService.generateToken(
                user.getUserId(), email, user.getRole().name(),user.getUsername()
        );

        String role = String.valueOf(user.getRole());

        handleRefreshToken(email);

        return new LoginResponse(jwt, generateNewRefreshToken(email), role);
    }

    public Object adminLogin(AdminLoginRequest request) {

        String jwt = jwtService.generateToken(
                request.getAdminId(),
                request.getEmail(),
                request.getAdminRole().name(),
                request.getUsername()
        );

        String role = String.valueOf(request.getAdminRole());

        handleRefreshToken(request.getEmail());

        return new LoginResponse(jwt, generateNewRefreshToken(request.getEmail()), role);
    }

    public Object trainerLogin(@Valid UserLoginDTO request) {

        String email = request.getEmail();
        User user = userRepo.getUserByEmail(email);

        if (user == null) {
            log.warn("Login failed – not found: {}", email);
            throw new InvalidCredentialsException(AUTH_FAILED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Wrong password for: {}", email);
            throw new InvalidCredentialsException(AUTH_FAILED);
        }

        if (!user.isVerified()) {
            log.info("Not verified, resending OTP: {}", email);
            otpService.generateOtp(user.getUserId());
            return new OtpPendingResponse("OTP sent to email", true);
        }

        if (user.getRole() == Role.LEARNER || user.getRole() == Role.SUPER_ADMIN) {
            throw new AccessDeniedException(UNAUTHORIZED_LOGIN);
        }

        if (user.getRole() == Role.TRAINER) {

            if (user.getStatus() == null) {
                return new OtpPendingResponse("Profile status not set. Contact support.", false);
            }

            switch (user.getStatus()) {
                case "PROFILE_PENDING":
                    return new OtpPendingResponse("Please complete your profile details.", false);

                case "APPROVAL_PENDING":
                    return new OtpPendingResponse("Your application is under admin review.", false);

                case "REJECTED":
                    return new OtpPendingResponse("Your application was rejected.", false);

                case "ACTIVE":
                    break;

                default:
                    return new OtpPendingResponse("Something went wrong.", false);
            }
        }

        log.info("Login successful: {}", email);

        String jwt = jwtService.generateToken(
                user.getUserId(), email, user.getRole().name(),user.getUsername()
        );

        String role = String.valueOf(user.getRole());

        handleRefreshToken(email);

        return new LoginResponse(jwt, generateNewRefreshToken(email), role);
    }


    private void handleRefreshToken(String email) {
        RefreshToken existing = refreshTokenRepo.findByEmail(email);
        if (existing != null) {
            refreshTokenRepo.delete(existing.getToken());
        }
    }

    private String generateNewRefreshToken(String email) {
        String refreshToken = UUID.randomUUID().toString();

        RefreshToken rt = new RefreshToken();
        rt.setToken(refreshToken);
        rt.setEmail(email);
        rt.setExpiry(Instant.now().plus(7, ChronoUnit.DAYS).getEpochSecond());

        refreshTokenRepo.save(rt);

        return refreshToken;
    }
}