package com.example.kodemilabs.service;

import com.example.kodemilabs.dto.request.LearnerRegistrationRequest;
import com.example.kodemilabs.dto.request.TrainerRegistrationRequest;
import com.example.kodemilabs.dto.response.LoginResponse;
import com.example.kodemilabs.enums.Role;
import com.example.kodemilabs.exceptions.login.RegistrationFailedException;
import com.example.kodemilabs.exceptions.login.UserNotFoundException;
import com.example.kodemilabs.exceptions.registration.EmailAlreadyExistsException;
import com.example.kodemilabs.feign.PaymentClient;
import com.example.kodemilabs.feign.UserClient;
import com.example.kodemilabs.model.RefreshToken;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.repository.RefreshTokenRepo;
import com.example.kodemilabs.repository.UserRepo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @InjectMocks
    private RegisterService registerService;

    @Mock private UserRepo userRepo;
    @Mock private OTPService otpService;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenRepo refreshTokenRepo;
    @Mock private UserClient userClient;
    @Mock private PaymentClient paymentClient;

    // ================= LEARNER REGISTER =================

    @Test
    void register_emailAlreadyExists_shouldThrow() {
        LearnerRegistrationRequest req = buildLearner();

        when(userRepo.getUserByEmail(req.getEmail())).thenReturn(new User());

        Executable ex = new Executable() {
            public void execute() {
                registerService.register(req);
            }
        };

        assertThrows(EmailAlreadyExistsException.class, ex);
    }

    @Test
    void register_success_shouldSaveAndSendOtp() {
        LearnerRegistrationRequest req = buildLearner();

        when(userRepo.getUserByEmail(req.getEmail())).thenReturn(null);

        registerService.register(req);

        verify(userRepo).save(any());
        verify(otpService).generateOtp(any());
    }

    @Test
    void register_saveFails_shouldThrow() {
        LearnerRegistrationRequest req = buildLearner();

        when(userRepo.getUserByEmail(req.getEmail())).thenReturn(null);
        doThrow(new RuntimeException()).when(userRepo).save(any());

        Executable ex = new Executable() {
            public void execute() {
                registerService.register(req);
            }
        };

        assertThrows(RegistrationFailedException.class, ex);
    }

    // ================= VERIFY OTP (LEARNER) =================

    @Test
    void verifyOtp_userNotFound_shouldThrow() {
        when(userRepo.getUserByEmail(any())).thenReturn(null);

        Executable ex = new Executable() {
            public void execute() {
                registerService.verifyOtpAndLogin("x@mail.com", "123");
            }
        };

        assertThrows(UserNotFoundException.class, ex);
    }

    @Test
    void verifyOtp_success() {
        User user = buildUser();
        user.setRole(Role.LEARNER);

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("jwt");
        when(refreshTokenRepo.findByEmail(any())).thenReturn(null);

        LoginResponse res = registerService.verifyOtpAndLogin(user.getEmail(), "123");

        assertNotNull(res);
        verify(userRepo).save(user);
        verify(userClient).addLearner(any());
        verify(paymentClient).createWallet(any());
    }

    @Test
    void verifyOtp_paymentFails_shouldStillReturn() {
        User user = buildUser();
        user.setRole(Role.LEARNER);

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("jwt");
        when(refreshTokenRepo.findByEmail(any())).thenReturn(null);

        doThrow(new RuntimeException()).when(paymentClient).createWallet(any());

        LoginResponse res = registerService.verifyOtpAndLogin(user.getEmail(), "123");

        assertNotNull(res); // should not fail
    }

    // ================= TRAINER REGISTER =================

    @Test
    void registerTrainer_emailExists_shouldThrow() {
        TrainerRegistrationRequest req = buildTrainer();

        when(userRepo.getUserByEmail(req.getEmail())).thenReturn(new User());

        Executable ex = new Executable() {
            public void execute() {
                registerService.registerTrainer(req);
            }
        };

        assertThrows(EmailAlreadyExistsException.class, ex);
    }

    @Test
    void registerTrainer_success() {
        TrainerRegistrationRequest req = buildTrainer();

        when(userRepo.getUserByEmail(req.getEmail())).thenReturn(null);

        registerService.registerTrainer(req);

        verify(userRepo).save(any());
        verify(otpService).generateOtp(any());
    }

    @Test
    void registerTrainer_saveFails_shouldThrow() {
        TrainerRegistrationRequest req = buildTrainer();

        when(userRepo.getUserByEmail(req.getEmail())).thenReturn(null);
        doThrow(new RuntimeException()).when(userRepo).save(any());

        Executable ex = new Executable() {
            public void execute() {
                registerService.registerTrainer(req);
            }
        };

        assertThrows(RegistrationFailedException.class, ex);
    }

    // ================= VERIFY TRAINER OTP =================

    @Test
    void verifyTrainerOtp_userNotFound_shouldThrow() {
        when(userRepo.getUserByEmail(any())).thenReturn(null);

        Executable ex = new Executable() {
            public void execute() {
                registerService.verifyTrainerOtp("x@mail.com", "123");
            }
        };

        assertThrows(UserNotFoundException.class, ex);
    }

    @Test
    void verifyTrainerOtp_success() {
        User user = buildUser();
        user.setRole(Role.TRAINER);

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("jwt");
        when(refreshTokenRepo.findByEmail(any())).thenReturn(null);

        LoginResponse res = registerService.verifyTrainerOtp(user.getEmail(), "123");

        assertNotNull(res);
        verify(userRepo).save(user);
        verify(paymentClient).createWallet(any());
    }

    @Test
    void verifyTrainerOtp_paymentFails_shouldStillReturn() {
        User user = buildUser();
        user.setRole(Role.TRAINER);

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("jwt");
        when(refreshTokenRepo.findByEmail(any())).thenReturn(null);

        doThrow(new RuntimeException()).when(paymentClient).createWallet(any());

        LoginResponse res = registerService.verifyTrainerOtp(user.getEmail(), "123");

        assertNotNull(res);
    }

    // ================= REFRESH TOKEN =================

    @Test
    void verifyOtp_existingRefreshToken_deleted() {
        User user = buildUser();
        user.setRole(Role.LEARNER);

        RefreshToken rt = new RefreshToken();
        rt.setToken("old");

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("jwt");
        when(refreshTokenRepo.findByEmail(any())).thenReturn(rt);

        registerService.verifyOtpAndLogin(user.getEmail(), "123");

        verify(refreshTokenRepo).delete("old");
        verify(refreshTokenRepo).save(any());
    }

    // ================= HELPERS =================

    private LearnerRegistrationRequest buildLearner() {
        LearnerRegistrationRequest r = new LearnerRegistrationRequest();
        r.setEmail("test@mail.com");
        r.setName("test");
        r.setUsername("user");
        r.setPasswordHash("Password@123");
        return r;
    }

    private TrainerRegistrationRequest buildTrainer() {
        TrainerRegistrationRequest r = new TrainerRegistrationRequest();
        r.setEmail("trainer@mail.com");
        r.setName("trainer");
        r.setUsername("trainer");
        r.setPassword("Password@123");
        return r;
    }

    private User buildUser() {
        User u = new User();
        u.setUserId("1");
        u.setEmail("test@mail.com");
        u.setUsername("user");
        u.setName("name");
        u.setActive(true);
        u.setVerified(false);
        return u;
    }
}