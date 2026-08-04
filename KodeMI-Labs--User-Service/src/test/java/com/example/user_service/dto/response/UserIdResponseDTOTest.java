package com.example.user_service.dto.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserIdResponseDTOTest {

    @Test
    void testAllArgsConstructor() {
        UserIdResponseDTO dto = new UserIdResponseDTO("user123");
        assertEquals("user123", dto.getUserId());
    }

    @Test
    void testNoArgsConstructor() {
        UserIdResponseDTO dto = new UserIdResponseDTO();
        assertNotNull(dto);
    }

    @Test
    void testSettersAndGetters() {
        UserIdResponseDTO dto = new UserIdResponseDTO();
        
        dto.setUserId("user456");
        assertEquals("user456", dto.getUserId());
        
        dto.setUserId("user789");
        assertEquals("user789", dto.getUserId());
    }

    @Test
    void testNullValue() {
        UserIdResponseDTO dto = new UserIdResponseDTO();
        assertNull(dto.getUserId());
    }

    @Test
    void testEmptyString() {
        UserIdResponseDTO dto = new UserIdResponseDTO();
        dto.setUserId("");
        assertEquals("", dto.getUserId());
    }
}
