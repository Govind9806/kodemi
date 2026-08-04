package com.example.kodemilabs.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.example.kodemilabs.exceptions.login.UserNotFoundException;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.model.UserFootPrint;
import com.example.kodemilabs.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class UserFootPrintServiceTest {

    @Mock
    private DynamoDBMapper dynamoDBMapper;

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserFootPrintService service;

    private User user;
    private UserFootPrint footPrint;

    @BeforeEach
    void setup() {
        user = new User();
        user.setUserId("user123");
        user.setEmail("test@example.com");

        footPrint = new UserFootPrint();
        footPrint.setEmail("test@example.com");

        // Add sample values (including unsafe HTML)
        footPrint.setBrowser("<script>alert(1)</script>");
        footPrint.setOs("<b>Windows</b>");
        footPrint.setCity("<img src=x onerror=alert(1)>");
    }

    // ✅ SUCCESS CASE
    @Test
    void save_shouldSanitizeAndSave_whenValid() {
        when(userRepo.getUserByEmail("test@example.com")).thenReturn(user);

        service.save(footPrint);

        assertEquals("user123", footPrint.getUserId());

        // Correct expectations
        assertEquals("", footPrint.getBrowser()); // script removed fully
        assertEquals("Windows", footPrint.getOs()); // tag removed, text kept
        assertEquals("", footPrint.getCity()); // img removed

        verify(dynamoDBMapper).save(footPrint);
    }

    // ❌ NULL FOOTPRINT
    @Test
    void save_shouldThrow_whenFootPrintIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> service.save(null));

        verify(dynamoDBMapper, never()).save(any());
    }

    // ❌ NULL EMAIL
    @Test
    void save_shouldThrow_whenEmailIsNull() {
        footPrint.setEmail(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.save(footPrint));
    }

    // ❌ USER NOT FOUND
    @Test
    void save_shouldThrow_whenUserNotFound() {
        when(userRepo.getUserByEmail("test@example.com")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> service.save(footPrint));

        verify(dynamoDBMapper, never()).save(any());
    }

    // ⚠️ NULL FIELDS SHOULD STAY NULL
    @Test
    void save_shouldHandleNullFieldsGracefully() {
        when(userRepo.getUserByEmail("test@example.com")).thenReturn(user);

        footPrint.setBrowser(null);
        footPrint.setOs(null);

        service.save(footPrint);

        assertNull(footPrint.getBrowser());
        assertNull(footPrint.getOs());

        verify(dynamoDBMapper).save(footPrint);
    }

    // ⚠️ PLAIN TEXT SHOULD REMAIN SAME
    @Test
    void save_shouldNotModifySafeText() {
        when(userRepo.getUserByEmail("test@example.com")).thenReturn(user);

        footPrint.setBrowser("Chrome");
        footPrint.setOs("Windows");

        service.save(footPrint);

        assertEquals("Chrome", footPrint.getBrowser());
        assertEquals("Windows", footPrint.getOs());
    }
}