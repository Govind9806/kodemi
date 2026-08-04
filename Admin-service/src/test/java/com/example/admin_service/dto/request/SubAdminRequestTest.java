package com.example.admin_service.dto.request;

import com.example.admin_service.enums.AdminRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SubAdminRequestTest {

    @Test
    void testGettersAndSetters() {
        SubAdminRequest dto = new SubAdminRequest();
        dto.setEmail("sub@domain.com");
        dto.setUsername("subUser");
        dto.setAdminRole(AdminRole.COURSE_ADMIN);

        assertEquals("sub@domain.com", dto.getEmail());
        assertEquals("subUser", dto.getUsername());
        assertEquals(AdminRole.COURSE_ADMIN, dto.getAdminRole());
    }
}
