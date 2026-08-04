package com.example.kodemilabs.service;
import com.example.kodemilabs.enums.Role;
import com.example.kodemilabs.exceptions.otp.InvalidTokenException;
import com.example.kodemilabs.exceptions.otp.TokenExpiredException;
import com.example.kodemilabs.model.RefreshToken;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.repository.RefreshTokenRepo;
import com.example.kodemilabs.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenService {

    private final RefreshTokenRepo refreshTokenRepo;
    private final JwtService jwtService;
    private final UserRepo userRepo;

    private static final String INVALID_REFRESH_TOKEN_MSG = "Invalid refresh token";
    private static final String EXPIRED_REFRESH_TOKEN_MSG = "Refresh token expired";
    private static final String USER_NOT_FOUND_MSG = "User not found";

    public RefreshTokenService(RefreshTokenRepo refreshTokenRepo,
                               JwtService jwtService,
                               UserRepo userRepo) {
        this.refreshTokenRepo = refreshTokenRepo;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
    }

    public Map<String, String> refreshAccessToken(String refreshToken) {

        log.info("Attempting to refresh access token");

        RefreshToken tokenRecord = refreshTokenRepo.find(refreshToken);
        if (tokenRecord == null) {
            log.warn("Invalid refresh token provided");
            throw new InvalidTokenException(INVALID_REFRESH_TOKEN_MSG);
        }

        long now = Instant.now().getEpochSecond();
        if (tokenRecord.getExpiry() < now) {
            log.warn("Refresh token expired for email: {}", maskEmail(tokenRecord.getEmail()));
            throw new TokenExpiredException(EXPIRED_REFRESH_TOKEN_MSG);
        }

        if (tokenRecord.isInvalidated()) {
            log.warn("Security Alert: Reused invalidated refresh token detected for email: {}. Revoking all sessions.", maskEmail(tokenRecord.getEmail()));
            refreshTokenRepo.deleteAllByEmail(tokenRecord.getEmail());
            throw new InvalidTokenException("Invalidated token reused. All sessions have been revoked. Please log in again.");
        }

        User user = userRepo.getUserByEmail(tokenRecord.getEmail());
        if (user == null) {
            log.warn("User not found for refresh token");
            throw new InvalidTokenException(USER_NOT_FOUND_MSG);
        }

        // 1. Invalidate the current used token (Rotation)
        tokenRecord.setInvalidated(true);
        refreshTokenRepo.save(tokenRecord);

        // 2. Generate new Access Token
        String newJwt = jwtService.generateToken(
                user.getUserId(),
                tokenRecord.getEmail(),
                user.getRole() != null ? user.getRole().name() : Role.LEARNER.name(),
                user.getUsername());

        // 3. Generate new Refresh Token
        String newRefreshToken = UUID.randomUUID().toString();
        RefreshToken newRt = new RefreshToken();
        newRt.setToken(newRefreshToken);
        newRt.setEmail(user.getEmail());
        newRt.setExpiry(Instant.now().plus(7, ChronoUnit.DAYS).getEpochSecond());
        newRt.setInvalidated(false);
        refreshTokenRepo.save(newRt);

        log.info("Access token refreshed successfully (rotated) for user {}", maskEmail(tokenRecord.getEmail()));

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", newJwt);
        response.put("refreshToken", newRefreshToken);
        return response;
    }


    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String namePart = parts[0];
        String domainPart = parts[1];
        String visible = namePart.length() <= 2 ? namePart : namePart.substring(0, 2);
        return visible + "***@" + domainPart;
    }
}
