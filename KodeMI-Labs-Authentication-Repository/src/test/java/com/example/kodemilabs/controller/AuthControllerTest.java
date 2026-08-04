package com.example.kodemilabs.controller;

import com.example.kodemilabs.config.RateLimitConfig;
import com.example.kodemilabs.dto.request.LearnerRegistrationRequest;
import com.example.kodemilabs.dto.request.ResendOtpRequest;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.model.UserFootPrint;
import com.example.kodemilabs.repository.UserRepo;
import com.example.kodemilabs.service.UserFootPrintService;
import com.example.kodemilabs.dto.request.UserLoginDTO;
import com.example.kodemilabs.dto.response.LoginResponse;
import com.example.kodemilabs.facade.AuthFacade;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock private AuthFacade authFacade;
    @Mock private RateLimitConfig.RateLimiter rateLimiter;
    @Mock private UserRepo userRepo;
    @Mock private UserFootPrintService userFootPrintService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setup() {
        validator = new LocalValidatorFactoryBean();
        ((LocalValidatorFactoryBean) validator).afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
    }

    // ================= LOGIN =================

    @Test
    void login_shouldReturnOk() throws Exception {
        UserLoginDTO request = new UserLoginDTO();
        request.setEmail("test@mail.com");
        request.setPassword("Password@123");

        when(authFacade.login(any()))
                .thenReturn(Map.of("token", "abc"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authFacade).login(any());
    }

    // ================= REGISTER =================

    @Test
    void register_shouldReturnCreated() throws Exception {
        LearnerRegistrationRequest request = new LearnerRegistrationRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setUsername("testuser");
        request.setPasswordHash("Password@123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("OTP sent to email"));

        verify(authFacade).register(any());
    }

    // ================= TRAINER LOGIN =================

    @Test
    void trainerLogin_shouldReturnOk() throws Exception {
        UserLoginDTO request = new UserLoginDTO();
        request.setEmail("trainer@mail.com");
        request.setPassword("Password@123");

        when(authFacade.trainerLogin(any()))
                .thenReturn(Map.of("token", "trainer-token"));

        mockMvc.perform(post("/api/v1/auth/trainer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authFacade).trainerLogin(any());
    }

    // ================= VERIFY OTP =================

    @Test
    void verifyOtp_success() throws Exception {
        when(rateLimiter.allowRequest(any())).thenReturn(true);

        when(authFacade.verifyOtpAndLogin(any(), any()))
                .thenReturn(new LoginResponse("access-token", "refresh-token", "LEARNER"));

        mockMvc.perform(post("/api/v1/auth/verify")
                        .param("email", "test@mail.com")
                        .param("otp", "123456"))
                .andExpect(status().isOk());

        verify(authFacade).verifyOtpAndLogin(any(), any());
    }

    @Test
    void verifyOtp_rateLimitExceeded() throws Exception {
        when(rateLimiter.allowRequest(any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/verify")
                        .param("email", "test@mail.com")
                        .param("otp", "123456"))
                .andExpect(status().isTooManyRequests());
    }

    // ================= RESEND OTP =================

    @Test
    void resendOtp_success() throws Exception {
        when(rateLimiter.allowRequest(any())).thenReturn(true);

        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail("test@mail.com");

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authFacade).resendOtp(any());
    }



    @Test
    void trainerAdminActions_submitted_success() throws Exception {
        User user = new User();
        user.setUserId("t1");
        user.setStatus("PROFILE_PENDING");

        when(userRepo.findById("t1")).thenReturn(java.util.Optional.of(user));

        mockMvc.perform(post("/api/v1/auth/internal/trainer/submitted/t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile submitted for admin review"));
    }

    @ParameterizedTest
    @CsvSource({
            "VERIFY, Trainer activated successfully",
            "REJECT, Trainer rejected"
    })
    void trainerAdminActions_review_success(String action, String expectedMessage) throws Exception {
        User user = new User();
        user.setUserId("t1");
        user.setStatus("APPROVAL_PENDING");

        when(userRepo.findById("t1")).thenReturn(java.util.Optional.of(user));

        mockMvc.perform(put("/api/v1/auth/internal/trainer/review/t1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"" + action + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }


    @Test
    void captureFootprint_success() throws Exception {
        UserFootPrint fp = new UserFootPrint();
        fp.setEmail("test@mail.com");

        mockMvc.perform(post("/api/v1/auth/footprint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fp)))
                .andExpect(status().isOk());

        verify(userFootPrintService).save(any());
    }

    // ================= REFRESH TOKEN =================

    @Test
    void refresh_shouldReturnOk() throws Exception {
        when(authFacade.refreshToken(any())).thenReturn(Map.of("accessToken", "new-jwt"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-token\"}"))
                .andExpect(status().isOk());

        verify(authFacade).refreshToken("old-token");
    }

    // ================= REQUEST RESET OTP =================

    @Test
    void requestResetOtp_success() throws Exception {
        when(rateLimiter.allowRequest(any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/request-reset-password-otp")
                        .param("email", "test@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reset OTP sent to email"));

        verify(authFacade).requestResetOtp("test@mail.com");
    }

    @Test
    void requestResetOtp_rateLimited() throws Exception {
        when(rateLimiter.allowRequest(any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/request-reset-password-otp")
                        .param("email", "test@mail.com"))
                .andExpect(status().isTooManyRequests());
    }

    // ================= VERIFY RESET OTP =================

    @Test
    void verifyResetPasswordOtp_success() throws Exception {
        when(authFacade.verifyResetOtp(any(), any())).thenReturn("reset-token-123");

        mockMvc.perform(post("/api/v1/auth/verify-reset-password-otp")
                        .param("email", "test@mail.com")
                        .param("otp", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").value("reset-token-123"));
    }

    @Test
    void verifyResetPasswordOtp_failure() throws Exception {
        when(authFacade.verifyResetOtp(any(), any())).thenThrow(new RuntimeException("expired"));

        mockMvc.perform(post("/api/v1/auth/verify-reset-password-otp")
                        .param("email", "test@mail.com")
                        .param("otp", "123456"))
                .andExpect(status().isBadRequest());
    }

    // ================= RESET PASSWORD =================

    @Test
    void resetPassword_success() throws Exception {
        com.example.kodemilabs.model.ResetPassword req = new com.example.kodemilabs.model.ResetPassword();
        req.setToken("token123");
        req.setNewPassword("NewPass@123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }

    @Test
    void resetPassword_failure() throws Exception {
        doThrow(new RuntimeException("Invalid token"))
                .when(authFacade).resetPassword(any(), any());

        com.example.kodemilabs.model.ResetPassword req = new com.example.kodemilabs.model.ResetPassword();
        req.setToken("bad-token");
        req.setNewPassword("NewPass@123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ================= REGISTER TRAINER =================

    @Test
    void registerTrainer_shouldReturnCreated() throws Exception {
        com.example.kodemilabs.dto.request.TrainerRegistrationRequest req =
                new com.example.kodemilabs.dto.request.TrainerRegistrationRequest();
        req.setName("Trainer");
        req.setEmail("trainer@mail.com");
        req.setUsername("trainer1");
        req.setPassword("Password@123");

        mockMvc.perform(post("/api/v1/auth/register/trainer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        verify(authFacade).registerTrainer(any());
    }

    // ================= VERIFY TRAINER OTP =================

    @Test
    void verifyTrainerOtp_success() throws Exception {
        when(rateLimiter.allowRequest(any())).thenReturn(true);
        when(authFacade.verifyTrainerOtp(any(), any()))
                .thenReturn(new LoginResponse("jwt", "rt", "TRAINER"));

        mockMvc.perform(post("/api/v1/auth/verify/trainer")
                        .param("email", "trainer@mail.com")
                        .param("otp", "123456"))
                .andExpect(status().isOk());

        verify(authFacade).verifyTrainerOtp(any(), any());
    }

    @Test
    void verifyTrainerOtp_rateLimited() throws Exception {
        when(rateLimiter.allowRequest(any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/verify/trainer")
                        .param("email", "trainer@mail.com")
                        .param("otp", "123456"))
                .andExpect(status().isTooManyRequests());
    }

    // ================= CHECK STATUS =================

    @Test
    void checkStatus_userFound() throws Exception {
        User user = new User();
        user.setRole(com.example.kodemilabs.enums.Role.LEARNER);
        user.setStatus("ACTIVE");
        user.setActive(true);

        when(userRepo.getUserByEmail("test@mail.com")).thenReturn(user);

        mockMvc.perform(get("/api/v1/auth/status")
                        .param("email", "test@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void checkStatus_userNotFound() throws Exception {
        when(userRepo.getUserByEmail(any())).thenReturn(null);

        mockMvc.perform(get("/api/v1/auth/status")
                        .param("email", "test@mail.com"))
                .andExpect(status().isNotFound());
    }

    // ================= PENDING TRAINERS =================

    @Test
    void getPendingTrainers_shouldReturnList() throws Exception {
        User user = new User();
        user.setUserId("t1");

        when(userRepo.getPendingTrainers()).thenReturn(java.util.List.of(user));

        mockMvc.perform(get("/api/v1/auth/pending/trainer"))
                .andExpect(status().isOk());
    }

    // ================= TRAINER REVIEW EDGE CASES =================

    @Test
    void reviewTrainer_invalidAction_shouldReturnBadRequest() throws Exception {
        User user = new User();
        user.setUserId("t1");
        when(userRepo.findById("t1")).thenReturn(java.util.Optional.of(user));

        mockMvc.perform(put("/api/v1/auth/internal/trainer/review/t1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendOtp_rateLimited() throws Exception {
        when(rateLimiter.allowRequest(any())).thenReturn(false);

        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail("test@mail.com");

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }
}