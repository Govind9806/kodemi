package com.example.ai_service.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void valuesShouldContainAllEnums() {

        Role[] values = Role.values();

        assertEquals(2, values.length);
        assertEquals(Role.USER, values[0]);
        assertEquals(Role.ASSISTANT, values[1]);
    }

    @Test
    void valueOfShouldWork() {

        assertEquals(
                Role.USER,
                Role.valueOf("USER")
        );

        assertEquals(
                Role.ASSISTANT,
                Role.valueOf("ASSISTANT")
        );
    }

    @Test
    void ordinalShouldBeCorrect() {

        assertEquals(0, Role.USER.ordinal());
        assertEquals(1, Role.ASSISTANT.ordinal());
    }

    @Test
    void toStringShouldMatchName() {

        assertEquals(
                "USER",
                Role.USER.toString()
        );

        assertEquals(
                "ASSISTANT",
                Role.ASSISTANT.toString()
        );
    }
}