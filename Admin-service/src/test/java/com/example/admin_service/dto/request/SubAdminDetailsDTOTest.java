package com.example.admin_service.dto.request;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SubAdminDetailsDTOTest {

    @Test
    void testGetters() {
        SubAdminDetailsDTO dto = new SubAdminDetailsDTO();
        // Since there is only @Getter, the fields will be initialized to null
        assertNull(dto.getUsername());
        assertNull(dto.getPassword());
    }
}
