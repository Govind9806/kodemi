package com.example.admin_service.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void testEnumValues() {
        Role[] roles = Role.values();

        assertEquals(3, roles.length);
        assertArrayEquals(
                new Role[]{Role.LEARNER, Role.SUPER_ADMIN, Role.TRAINER},
                roles
        );
    }

    @Test
    void testValueOf() {
        assertEquals(Role.LEARNER, Role.valueOf("LEARNER"));
        assertEquals(Role.SUPER_ADMIN, Role.valueOf("SUPER_ADMIN"));
        assertEquals(Role.TRAINER, Role.valueOf("TRAINER"));
    }

    @Test
    void testInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            Role.valueOf("INVALID_ROLE");
        });
    }

    @Test
    void testEnumNames() {
        assertEquals("LEARNER", Role.LEARNER.name());
        assertEquals("SUPER_ADMIN", Role.SUPER_ADMIN.name());
        assertEquals("TRAINER", Role.TRAINER.name());
    }

    @Test
    void testOrdinalValues() {
        assertEquals(0, Role.LEARNER.ordinal());
        assertEquals(1, Role.SUPER_ADMIN.ordinal());
        assertEquals(2, Role.TRAINER.ordinal());
    }
}