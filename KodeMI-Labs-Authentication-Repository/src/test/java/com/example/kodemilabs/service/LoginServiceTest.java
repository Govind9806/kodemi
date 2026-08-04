package com.example.kodemilabs.service;

import com.example.kodemilabs.dto.request.AdminLoginRequest;
import com.example.kodemilabs.dto.request.UserLoginDTO;
import com.example.kodemilabs.dto.response.LoginResponse;
import com.example.kodemilabs.dto.response.OtpPendingResponse;
import com.example.kodemilabs.enums.AdminRole;
import com.example.kodemilabs.enums.Role;
import com.example.kodemilabs.exceptions.jwt.AccessDeniedException;
import com.example.kodemilabs.exceptions.login.InvalidCredentialsException;
import com.example.kodemilabs.model.RefreshToken;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.repository.RefreshTokenRepo;
import com.example.kodemilabs.repository.UserRepo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @InjectMocks
    private LoginService loginService;

    @Mock private OTPService otpService;
    @Mock private UserRepo userRepo;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenRepo refreshTokenRepo;

    // ================= LOGIN =================

    @Test
    void login_userNotFound() {
        when(userRepo.getUserByEmail(any())).thenReturn(null);

        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("x@mail.com");

        Executable ex = new Executable() {
            public void execute() {
                loginService.login(dto);
            }
        };

        assertThrows(InvalidCredentialsException.class, ex);
    }

    @Test
    void login_notVerified() {
        User user = buildUser(Role.LEARNER, false);
        mockUser(user);

        Object res = loginService.login(buildLogin(user));

        assertInstanceOf(OtpPendingResponse.class, res);
        verify(otpService).generateOtp(user.getUserId());
    }

    @Test
    void login_trainerAccessDenied() {
        User user = buildUser(Role.TRAINER, true);
        mockUser(user);

        Executable ex = new Executable() {
            public void execute() {
                loginService.login(buildLogin(user));
            }
        };

        assertThrows(AccessDeniedException.class, ex);
    }

    @Test
    void login_adminAccessDenied() {
        User user = buildUser(Role.SUPER_ADMIN, true);
        mockUser(user);

        Executable ex = new Executable() {
            public void execute() {
                loginService.login(buildLogin(user));
            }
        };

        assertThrows(AccessDeniedException.class, ex);
    }

    @Test
    void login_wrongPassword() {
        User user = buildUser(Role.LEARNER, true);
        user.setPasswordHash(BCrypt.hashpw("correct", BCrypt.gensalt()));

        mockUser(user);

        UserLoginDTO dto = buildLogin(user);
        dto.setPassword("wrong");

        Executable ex = new Executable() {
            public void execute() {
                loginService.login(dto);
            }
        };

        assertThrows(InvalidCredentialsException.class, ex);
    }

    @Test
    void login_success_noExistingToken() {
        User user = buildUser(Role.LEARNER, true);
        user.setPasswordHash(BCrypt.hashpw("Password@123", BCrypt.gensalt()));

        mockUser(user);
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("jwt");
        when(refreshTokenRepo.findByEmail(any())).thenReturn(null);

        Object res = loginService.login(buildLogin(user));

        assertInstanceOf(LoginResponse.class, res);
        verify(refreshTokenRepo).save(any());
    }

    @Test
    void login_success_existingToken_deleted() {
        User user = buildUser(Role.LEARNER, true);
        user.setPasswordHash(BCrypt.hashpw("Password@123", BCrypt.gensalt()));

        RefreshToken rt = new RefreshToken();
        rt.setToken("old");

        mockUser(user);
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("jwt");
        when(refreshTokenRepo.findByEmail(any())).thenReturn(rt);

        loginService.login(buildLogin(user));

        verify(refreshTokenRepo).delete("old");
        verify(refreshTokenRepo).save(any());
    }

    // ================= ADMIN =================

    @Test
    void adminLogin_success() {
        AdminLoginRequest req = buildAdmin();

        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("jwt");
        when(refreshTokenRepo.findByEmail(any())).thenReturn(null);

        Object res = loginService.adminLogin(req);

        assertInstanceOf(LoginResponse.class, res);
        verify(refreshTokenRepo).save(any());
    }

    @Test
    void adminLogin_existingToken_deleted() {
        AdminLoginRequest req = buildAdmin();

        RefreshToken rt = new RefreshToken();
        rt.setToken("old");

        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("jwt");
        when(refreshTokenRepo.findByEmail(any())).thenReturn(rt);

        loginService.adminLogin(req);

        verify(refreshTokenRepo).delete("old");
    }

    // ================= TRAINER =================

    @Test
    void trainer_userNotFound() {
        when(userRepo.getUserByEmail(any())).thenReturn(null);

        Executable ex = new Executable() {
            public void execute() {
                loginService.trainerLogin(new UserLoginDTO());
            }
        };

        assertThrows(InvalidCredentialsException.class, ex);
    }

    @Test
    void trainer_notVerified() {
        User user = buildUser(Role.TRAINER, false);
        mockUser(user);

        Object res = loginService.trainerLogin(buildLogin(user));

        assertInstanceOf(OtpPendingResponse.class, res);
        verify(otpService).generateOtp(user.getUserId());
    }

    @Test
    void trainer_wrongRole_shouldThrow() {
        User user = buildUser(Role.LEARNER, true);
        mockUser(user);

        Executable ex = new Executable() {
            public void execute() {
                loginService.trainerLogin(buildLogin(user));
            }
        };

        assertThrows(AccessDeniedException.class, ex);
    }

    // ✅ Parameterized (no lambda needed here)
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"PROFILE_PENDING", "APPROVAL_PENDING", "REJECTED"})
    void trainer_status_shouldReturnOtpPending(String status) {
        User user = buildUser(Role.TRAINER, true);
        user.setStatus(status);

        mockUser(user);

        Object res = loginService.trainerLogin(buildLogin(user));

        assertInstanceOf(OtpPendingResponse.class, res);
    }

    @Test
    void trainer_success_active() {
        User user = buildUser(Role.TRAINER, true);
        user.setStatus("ACTIVE");
        user.setPasswordHash(BCrypt.hashpw("Password@123", BCrypt.gensalt()));

        mockUser(user);
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("jwt");

        Object res = loginService.trainerLogin(buildLogin(user));

        assertInstanceOf(LoginResponse.class, res);
    }

    @Test
    void trainer_status_default_shouldReturnOtpPending() {
        User user = buildUser(Role.TRAINER, true);
        user.setStatus("UNKNOWN_STATUS");
        mockUser(user);

        Object res = loginService.trainerLogin(buildLogin(user));

        assertInstanceOf(OtpPendingResponse.class, res);
    }

    @Test
    void trainer_superAdmin_shouldThrow() {
        User user = buildUser(Role.SUPER_ADMIN, true);
        mockUser(user);

        Executable ex = new Executable() {
            public void execute() {
                loginService.trainerLogin(buildLogin(user));
            }
        };

        assertThrows(AccessDeniedException.class, ex);
    }

    // ================= HELPERS =================

    private void mockUser(User user) {
        when(userRepo.getUserByEmail(any())).thenReturn(user);
    }

    private User buildUser(Role role, boolean verified) {
        User u = new User();
        u.setUserId(UUID.randomUUID().toString());
        u.setEmail("test@mail.com");
        u.setUsername("user");
        u.setRole(role);
        u.setVerified(verified);
        u.setPasswordHash(BCrypt.hashpw("Password@123", BCrypt.gensalt()));
        return u;
    }

    private UserLoginDTO buildLogin(User u) {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail(u.getEmail());
        dto.setPassword("Password@123");
        return dto;
    }

    private AdminLoginRequest buildAdmin() {
        return AdminLoginRequest.builder()
                .adminId("1")
                .email("admin@mail.com")
                .username("admin")
                .adminRole(AdminRole.SUPER_ADMIN)
                .build();
    }
}