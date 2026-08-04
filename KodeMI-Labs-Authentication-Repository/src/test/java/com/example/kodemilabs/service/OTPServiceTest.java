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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OTPServiceTest {

    @InjectMocks
    private OTPService otpService;

    @Mock private EmailService emailService;
    @Mock private OTPRepo otpRepo;
    @Mock private UserRepo userRepo;

    // ================= GENERATE =================

    @Test
    void generateOtp_userNotFound() {
        when(userRepo.getUserById(any())).thenReturn(null);

        Executable ex = new Executable() {
            public void execute() {
                otpService.generateOtp("1");
            }
        };

        assertThrows(UserNotFoundException.class, ex);
    }

    @Test
    void generateOtp_success() {
        User user = buildUser();
        when(userRepo.getUserById(user.getUserId())).thenReturn(user);

        otpService.generateOtp(user.getUserId());

        verify(otpRepo).save(any());
        verify(emailService).sendOtpEmail(eq(user.getEmail()), anyString());
    }

    // ================= RESET OTP =================

    @Test
    void resetOtp_userNotFound() {
        when(userRepo.getUserByEmail(any())).thenReturn(null);

        Executable ex = new Executable() {
            public void execute() {
                otpService.generateResetPasswordOtp("x@mail.com");
            }
        };

        assertThrows(UserNotFoundException.class, ex);
    }

    @Test
    void resetOtp_existingOtp_deleted() {
        User user = buildUser();
        OTP otp = buildOtp(user, "123456");

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(otpRepo.getOtpData(user.getUserId())).thenReturn(otp);

        otpService.generateResetPasswordOtp(user.getEmail());

        verify(otpRepo).delete(otp);
        verify(otpRepo).save(any());
    }

    // ================= VERIFY =================

    @Test
    void verifyOtp_userLocked() {
        User user = buildUser();
        user.setLockedUntil(System.currentTimeMillis() + 10000);

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);

        Executable ex = new Executable() {
            public void execute() {
                otpService.verifyOtp(user.getEmail(), "123456");
            }
        };

        assertThrows(AccountLockedException.class, ex);
    }

    @Test
    void verifyOtp_otpNull() {
        User user = buildUser();

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(otpRepo.getOtpData(user.getUserId())).thenReturn(null);

        Executable ex = new Executable() {
            public void execute() {
                otpService.verifyOtp(user.getEmail(), "123456");
            }
        };

        assertThrows(InvalidTokenException.class, ex);
    }

    @Test
    void verifyOtp_alreadyUsed() {
        User user = buildUser();
        OTP otp = buildOtp(user, "123456");
        otp.setEnable(false);

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(otpRepo.getOtpData(user.getUserId())).thenReturn(otp);

        Executable ex = new Executable() {
            public void execute() {
                otpService.verifyOtp(user.getEmail(), "123456");
            }
        };

        assertThrows(OtpAlreadyUsedException.class, ex);
    }

    @Test
    void verifyOtp_expired() {
        User user = buildUser();
        OTP otp = buildOtp(user, "123456");
        otp.setExpireAt(System.currentTimeMillis() - 1000);

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(otpRepo.getOtpData(user.getUserId())).thenReturn(otp);

        Executable ex = new Executable() {
            public void execute() {
                otpService.verifyOtp(user.getEmail(), "123456");
            }
        };

        assertThrows(TokenExpiredException.class, ex);
        verify(otpRepo).delete(otp);
    }

    @Test
    void verifyOtp_wrongOtp_incrementsAttempts() {
        User user = buildUser();
        OTP otp = buildOtp(user, "123456");

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(otpRepo.getOtpData(user.getUserId())).thenReturn(otp);

        Executable ex = new Executable() {
            public void execute() {
                otpService.verifyOtp(user.getEmail(), "wrong");
            }
        };

        assertThrows(InvalidTokenException.class, ex);
        verify(otpRepo).save(otp);
    }

    @Test
    void verifyOtp_lockAfterFiveAttempts() {
        User user = buildUser();
        OTP otp = buildOtp(user, "123456");
        otp.setFailedAttempts(4);

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(otpRepo.getOtpData(user.getUserId())).thenReturn(otp);

        Executable ex = new Executable() {
            public void execute() {
                otpService.verifyOtp(user.getEmail(), "wrong");
            }
        };

        assertThrows(AccountLockedException.class, ex);
        verify(userRepo).save(user);
        verify(otpRepo).save(otp);
    }

    @Test
    void verifyOtp_success() {
        User user = buildUser();
        OTP otp = buildOtp(user, "123456");

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(otpRepo.getOtpData(user.getUserId())).thenReturn(otp);

        otpService.verifyOtp(user.getEmail(), "123456");

        assertFalse(otp.isEnable());
        verify(otpRepo).save(otp);
    }

    // ================= RESEND =================

    @Test
    void resendOtp_userNotFound() {
        ResendOtpRequest req = new ResendOtpRequest();
        req.setEmail("x@mail.com");

        when(userRepo.getUserByEmail(req.getEmail())).thenReturn(null);

        Executable ex = new Executable() {
            public void execute() {
                otpService.resendOtp(req);
            }
        };

        assertThrows(UserNotFoundException.class, ex);
    }

    @Test
    void resendOtp_rateLimit() {
        User user = buildUser();
        OTP otp = buildOtp(user, "123456");

        // simulate OTP created 30 sec ago
        otp.setExpireAt(System.currentTimeMillis() + (5 * 60 * 1000L) - 30000);

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(otpRepo.getOtpData(user.getUserId())).thenReturn(otp);

        ResendOtpRequest req = new ResendOtpRequest();
        req.setEmail(user.getEmail());

        Executable ex = new Executable() {
            public void execute() {
                otpService.resendOtp(req);
            }
        };

        assertThrows(TooManyRequestsException.class, ex);
    }

    @Test
    void resendOtp_success() {
        User user = buildUser();
        OTP otp = buildOtp(user, "123456");

        // simulate OTP created long ago (>60 sec)
        otp.setExpireAt(System.currentTimeMillis() - 60000);

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(otpRepo.getOtpData(user.getUserId())).thenReturn(otp);

        when(userRepo.getUserById(user.getUserId())).thenReturn(user);

        ResendOtpRequest req = new ResendOtpRequest();
        req.setEmail(user.getEmail());

        otpService.resendOtp(req);

        verify(otpRepo).delete(otp);
        verify(otpRepo, atLeastOnce()).save(any());
        verify(emailService).sendOtpEmail(eq(user.getEmail()), anyString());
    }

    // ================= HELPERS =================

    private User buildUser() {
        User u = new User();
        u.setUserId("1");
        u.setEmail("test@mail.com");
        return u;
    }

    private OTP buildOtp(User user, String raw) {
        OTP otp = new OTP();
        otp.setUserId(user.getUserId());
        otp.setOtpCode(BCrypt.hashpw(raw, BCrypt.gensalt()));
        otp.setEnable(true);
        otp.setExpireAt(System.currentTimeMillis() + 60000);
        otp.setFailedAttempts(0);
        return otp;
    }

    @Test
    void generateOtp_nullUserId_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> otpService.generateOtp(null));
    }

    @Test
    void resendOtp_noExistingOtp_shouldSendDirectly() {
        User user = buildUser();

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(otpRepo.getOtpData(user.getUserId())).thenReturn(null);
        when(userRepo.getUserById(user.getUserId())).thenReturn(user);

        ResendOtpRequest req = new ResendOtpRequest();
        req.setEmail(user.getEmail());

        otpService.resendOtp(req);

        verify(otpRepo, never()).delete(any());
        verify(emailService).sendOtpEmail(eq(user.getEmail()), anyString());
    }

    @Test
    void verifyResetPasswordOtp_success() {
        User user = buildUser();
        OTP otp = buildOtp(user, "654321");

        when(userRepo.getUserByEmail(user.getEmail())).thenReturn(user);
        when(otpRepo.getOtpData(user.getUserId())).thenReturn(otp);

        otpService.verifyResetPasswordOtp(user.getEmail(), "654321");

        assertFalse(otp.isEnable());
        verify(otpRepo).save(otp);
    }
}