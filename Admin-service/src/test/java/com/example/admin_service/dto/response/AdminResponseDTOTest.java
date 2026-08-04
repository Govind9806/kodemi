package com.example.admin_service.dto.response;

import com.example.admin_service.enums.AdminRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AdminResponseDTOTest {

    @Test
    void testGettersSettersAndConstructors() {
        AdminResponseDTO dto = new AdminResponseDTO();
        dto.setAdminId("a1");
        dto.setEmail("test@email.com");
        dto.setUsername("testuser");
        dto.setAdminRole(AdminRole.USER_ADMIN);

        assertEquals("a1", dto.getAdminId());
        assertEquals("test@email.com", dto.getEmail());
        assertEquals("testuser", dto.getUsername());
        assertEquals(AdminRole.USER_ADMIN, dto.getAdminRole());

        AdminResponseDTO allArgsDto = new AdminResponseDTO("a2", "user2@email.com", "user2", AdminRole.COURSE_ADMIN);
        assertEquals("a2", allArgsDto.getAdminId());
        assertEquals("user2@email.com", allArgsDto.getEmail());
        assertEquals("user2", allArgsDto.getUsername());
        assertEquals(AdminRole.COURSE_ADMIN, allArgsDto.getAdminRole());

        AdminResponseDTO builderDto = AdminResponseDTO.builder()
                .adminId("a3")
                .email("user3@email.com")
                .username("user3")
                .adminRole(null)
                .build();
        assertEquals("a3", builderDto.getAdminId());
        assertNull(builderDto.getAdminRole());
    }
}
