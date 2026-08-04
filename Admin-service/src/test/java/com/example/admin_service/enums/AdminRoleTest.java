package com.example.admin_service.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminRoleTest {

    @Test
    void testEnumValuesExist() {
        AdminRole[] roles = AdminRole.values();

        assertEquals(4, roles.length);
        assertArrayEquals(
                new AdminRole[]{
                        AdminRole.SUPER_ADMIN,
                        AdminRole.USER_ADMIN,
                        AdminRole.COURSE_ADMIN,
                        AdminRole.PAYMENT_ADMIN
                },
                roles
        );
    }

    @Test
    void testValueOfValid() {
        assertEquals(AdminRole.SUPER_ADMIN, AdminRole.valueOf("SUPER_ADMIN"));
        assertEquals(AdminRole.USER_ADMIN, AdminRole.valueOf("USER_ADMIN"));
        assertEquals(AdminRole.COURSE_ADMIN, AdminRole.valueOf("COURSE_ADMIN"));
        assertEquals(AdminRole.PAYMENT_ADMIN, AdminRole.valueOf("PAYMENT_ADMIN"));
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            AdminRole.valueOf("INVALID_ROLE");
        });
    }

    @Test
    void testToString() {
        assertEquals("SUPER_ADMIN", AdminRole.SUPER_ADMIN.toString());
        assertEquals("USER_ADMIN", AdminRole.USER_ADMIN.toString());
        assertEquals("COURSE_ADMIN", AdminRole.COURSE_ADMIN.toString());
        assertEquals("PAYMENT_ADMIN", AdminRole.PAYMENT_ADMIN.toString());
    }

    @Test
    void testEnumOrdinals() {
        assertEquals(0, AdminRole.SUPER_ADMIN.ordinal());
        assertEquals(1, AdminRole.USER_ADMIN.ordinal());
        assertEquals(2, AdminRole.COURSE_ADMIN.ordinal());
        assertEquals(3, AdminRole.PAYMENT_ADMIN.ordinal());
    }
}