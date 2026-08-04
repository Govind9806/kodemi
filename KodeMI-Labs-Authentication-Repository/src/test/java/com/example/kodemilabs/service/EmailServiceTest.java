package com.example.kodemilabs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @BeforeEach
    void setup() throws Exception {
        // Inject @Value manually
        Field field = EmailService.class.getDeclaredField("fromEmail");
        field.setAccessible(true);
        field.set(emailService, "test@kodemi.com");
    }

    // ================= VALIDATION =================

    @Test
    void sendOtpEmail_nullEmail_shouldThrow() {
        Executable ex = new Executable() {
            public void execute() {
                emailService.sendOtpEmail(null, "123456");
            }
        };

        assertThrows(IllegalArgumentException.class, ex);
    }

    @Test
    void sendOtpEmail_blankEmail_shouldThrow() {
        Executable ex = new Executable() {
            public void execute() {
                emailService.sendOtpEmail("   ", "123456");
            }
        };

        assertThrows(IllegalArgumentException.class, ex);
    }

    @Test
    void sendOtpEmail_nullOtp_shouldThrow() {
        Executable ex = new Executable() {
            public void execute() {
                emailService.sendOtpEmail("test@mail.com", null);
            }
        };

        assertThrows(IllegalArgumentException.class, ex);
    }

    @Test
    void sendOtpEmail_blankOtp_shouldThrow() {
        Executable ex = new Executable() {
            public void execute() {
                emailService.sendOtpEmail("test@mail.com", " ");
            }
        };

        assertThrows(IllegalArgumentException.class, ex);
    }

    // ================= SUCCESS =================

    @Test
    void sendOtpEmail_success_shouldSendMail() {
        emailService.sendOtpEmail("test@mail.com", "123456");

        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage msg = messageCaptor.getValue();

        assertEquals("test@kodemi.com", msg.getFrom());
        assertEquals("test@mail.com", msg.getTo()[0]);
        assertEquals("KodeMI - OTP Verification", msg.getSubject());
        assertTrue(msg.getText().contains("123456"));
    }

    // ================= FAILURE =================

    @Test
    void sendOtpEmail_mailSenderThrows_shouldPropagate() {
        doThrow(new RuntimeException("Mail failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        Executable ex = new Executable() {
            public void execute() {
                emailService.sendOtpEmail("test@mail.com", "123456");
            }
        };

        assertThrows(RuntimeException.class, ex);
    }

    // ================= EDGE CASE =================

    @Test
    void sendOtpEmail_longOtp_shouldStillWork() {
        String longOtp = "123456789012345";

        emailService.sendOtpEmail("test@mail.com", longOtp);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}