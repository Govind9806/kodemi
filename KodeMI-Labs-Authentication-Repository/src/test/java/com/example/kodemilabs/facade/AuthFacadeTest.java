package com.example.kodemilabs.facade;

import com.example.kodemilabs.dto.request.LearnerRegistrationRequest;
import com.example.kodemilabs.dto.request.ResendOtpRequest;
import com.example.kodemilabs.dto.request.TrainerRegistrationRequest;
import com.example.kodemilabs.dto.request.UserLoginDTO;
import com.example.kodemilabs.dto.response.LoginResponse;
import com.example.kodemilabs.service.LoginService;
import com.example.kodemilabs.service.OTPService;
import com.example.kodemilabs.service.PasswordResetService;
import com.example.kodemilabs.service.RefreshTokenService;
import com.example.kodemilabs.service.RegisterService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthFacadeTest {

    @InjectMocks
    private AuthFacade authFacade;

    @Mock private RegisterService registerService;
    @Mock private LoginService loginService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private OTPService otpService;
    @Mock private PasswordResetService passwordResetService;

    // ========== register ==========

    @Test
    void register_delegatesToRegisterService() {
        LearnerRegistrationRequest request = new LearnerRegistrationRequest();
        authFacade.register(request);
        verify(registerService).register(request);
    }

    // ========== login ==========

    @Test
    void login_delegatesToLoginService_andReturnsResult() {
        UserLoginDTO dto = new UserLoginDTO();
        LoginResponse expected = new LoginResponse("jwt", "rt", "LEARNER");
        when(loginService.login(dto)).thenReturn(expected);

        Object result = authFacade.login(dto);

        assertEquals(expected, result);
        verify(loginService).login(dto);
    }

    // ========== trainerLogin ==========

    @Test
    void trainerLogin_delegatesToLoginService_andReturnsResult() {
        UserLoginDTO dto = new UserLoginDTO();
        LoginResponse expected = new LoginResponse("jwt", "rt", "TRAINER");
        when(loginService.trainerLogin(dto)).thenReturn(expected);

        Object result = authFacade.trainerLogin(dto);

        assertEquals(expected, result);
        verify(loginService).trainerLogin(dto);
    }

    // ========== verifyOtpAndLogin ==========

    @Test
    void verifyOtpAndLogin_delegatesToRegisterService() {
        LoginResponse expected = new LoginResponse("jwt", "rt", "LEARNER");
        when(registerService.verifyOtpAndLogin("email@test.com", "123456")).thenReturn(expected);

        Object result = authFacade.verifyOtpAndLogin("email@test.com", "123456");

        assertEquals(expected, result);
        verify(registerService).verifyOtpAndLogin("email@test.com", "123456");
    }

    // ========== refreshToken ==========

    @Test
    void refreshToken_delegatesToRefreshTokenService() {
        Map<String, String> expected = Map.of("accessToken", "new-jwt");
        when(refreshTokenService.refreshAccessToken("old-token")).thenReturn(expected);

        Object result = authFacade.refreshToken("old-token");

        assertEquals(expected, result);
        verify(refreshTokenService).refreshAccessToken("old-token");
    }

    // ========== resendOtp ==========

    @Test
    void resendOtp_delegatesToOtpService() {
        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail("test@test.com");

        authFacade.resendOtp(request);

        verify(otpService).resendOtp(request);
    }

    // ========== requestResetOtp ==========

    @Test
    void requestResetOtp_delegatesToPasswordResetService() {
        authFacade.requestResetOtp("user@test.com");
        verify(passwordResetService).requestResetPasswordOtp("user@test.com");
    }

    // ========== verifyResetOtp ==========

    @Test
    void verifyResetOtp_delegatesToPasswordResetService_andReturnsToken() {
        when(passwordResetService.verifyResetPasswordOtp("user@test.com", "654321"))
                .thenReturn("reset-token-abc");

        String result = authFacade.verifyResetOtp("user@test.com", "654321");

        assertEquals("reset-token-abc", result);
        verify(passwordResetService).verifyResetPasswordOtp("user@test.com", "654321");
    }

    // ========== resetPassword ==========

    @Test
    void resetPassword_delegatesToPasswordResetService() {
        authFacade.resetPassword("reset-token-abc", "NewPass@123");
        verify(passwordResetService).resetPassword("reset-token-abc", "NewPass@123");
    }

    // ========== registerTrainer ==========

    @Test
    void registerTrainer_delegatesToRegisterService() {
        TrainerRegistrationRequest request = new TrainerRegistrationRequest();
        authFacade.registerTrainer(request);
        verify(registerService).registerTrainer(request);
    }

    // ========== verifyTrainerOtp ==========

    @Test
    void verifyTrainerOtp_delegatesToRegisterService_andReturnsResult() {
        LoginResponse expected = new LoginResponse("jwt", "rt", "TRAINER");
        when(registerService.verifyTrainerOtp("trainer@test.com", "123456")).thenReturn(expected);

        Object result = authFacade.verifyTrainerOtp("trainer@test.com", "123456");

        assertEquals(expected, result);
        verify(registerService).verifyTrainerOtp("trainer@test.com", "123456");
    }
}
