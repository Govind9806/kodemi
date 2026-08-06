package com.example.user_service.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleTest {

    @Test
    void testEnumValues() {
        Role[] roles = Role.values();

        assertEquals(9, roles.length);
        assertArrayEquals(
                new Role[]{Role.LEARNER, Role.SUPER_ADMIN, Role.INSTRUCTOR, Role.TRAINER, Role.ADMIN, Role.USER_ADMIN, Role.COURSE_ADMIN, Role.GAME_ADMIN,Role.CONTENT_ADMIN},
                roles
        );
    }

    @Test
    void testValueOf() {
        assertEquals(Role.LEARNER, Role.valueOf("LEARNER"));
        assertEquals(Role.SUPER_ADMIN, Role.valueOf("SUPER_ADMIN"));
        assertEquals(Role.INSTRUCTOR, Role.valueOf("INSTRUCTOR"));
        assertEquals(Role.TRAINER, Role.valueOf("TRAINER"));
        assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
        assertEquals(Role.USER_ADMIN, Role.valueOf("USER_ADMIN"));
        assertEquals(Role.COURSE_ADMIN, Role.valueOf("COURSE_ADMIN"));
        assertEquals(Role.GAME_ADMIN, Role.valueOf("GAME_ADMIN"));
        assertEquals(Role.CONTENT_ADMIN, Role.valueOf("CONTENT_ADMIN"));
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
        assertEquals("INSTRUCTOR", Role.INSTRUCTOR.name());
        assertEquals("TRAINER", Role.TRAINER.name());
        assertEquals("ADMIN", Role.ADMIN.name());
        assertEquals("USER_ADMIN", Role.USER_ADMIN.name());
        assertEquals("COURSE_ADMIN", Role.COURSE_ADMIN.name());
        assertEquals("CONTENT_ADMIN", Role.CONTENT_ADMIN.name());
    }

    @Test
    void testOrdinalValues() {
        assertEquals(0, Role.LEARNER.ordinal());
        assertEquals(1, Role.SUPER_ADMIN.ordinal());
        assertEquals(2, Role.INSTRUCTOR.ordinal());
        assertEquals(3, Role.TRAINER.ordinal());
        assertEquals(4, Role.ADMIN.ordinal());
        assertEquals(5, Role.USER_ADMIN.ordinal());
        assertEquals(6, Role.COURSE_ADMIN.ordinal());
        assertEquals(7, Role.GAME_ADMIN.ordinal());
        assertEquals(8, Role.CONTENT_ADMIN.ordinal());
    }
}