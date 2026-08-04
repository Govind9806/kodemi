package com.example.kodemilabs.facade;

import com.example.kodemilabs.dto.request.LearnerRegistrationRequest;
import com.example.kodemilabs.dto.request.TrainerRegistrationRequest;
import com.example.kodemilabs.dto.request.ResendOtpRequest;
import com.example.kodemilabs.dto.request.UserLoginDTO;
import com.example.kodemilabs.service.*;
import org.springframework.stereotype.Service;

@Service
public class AuthFacade {


    private final RegisterService registerService;
    private final LoginService loginService;
    private final RefreshTokenService refreshTokenService;
    private final OTPService otpService;
    private final PasswordResetService passwordResetService;

    public AuthFacade(RegisterService registerService,
                      LoginService loginService,
                      RefreshTokenService refreshTokenService,
                      OTPService otpService,
                      PasswordResetService passwordResetService) {
        this.registerService = registerService;
        this.loginService = loginService;
        this.refreshTokenService = refreshTokenService;
        this.otpService = otpService;
        this.passwordResetService = passwordResetService;
    }

    // expose only required methods

    public void register(LearnerRegistrationRequest request) {
        registerService.register(request);
    }

    public Object login(UserLoginDTO request) {
        return loginService.login(request);
    }

    public Object trainerLogin(UserLoginDTO request) {
        return loginService.trainerLogin(request);
    }

    public Object verifyOtpAndLogin(String email, String otp) {
        return registerService.verifyOtpAndLogin(email, otp);
    }

    public Object refreshToken(String token) {
        return refreshTokenService.refreshAccessToken(token);
    }

    public void resendOtp(ResendOtpRequest request) {
        otpService.resendOtp(request);
    }

    public void requestResetOtp(String email) {
        passwordResetService.requestResetPasswordOtp(email);
    }

    public String verifyResetOtp(String email, String otp) {
        return passwordResetService.verifyResetPasswordOtp(email, otp);
    }

    public void resetPassword(String token, String password) {
        passwordResetService.resetPassword(token, password);
    }

    public void registerTrainer(TrainerRegistrationRequest request) {
        registerService.registerTrainer(request);
    }

    public Object verifyTrainerOtp(String email, String otp) {
        return registerService.verifyTrainerOtp(email, otp);
    }
}