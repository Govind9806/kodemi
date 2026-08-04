package com.example.user_service.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountStatusTest {

    @Test
    void testEnumValues() {
        AccountStatus[] values = AccountStatus.values();
        assertNotNull(values);
        assertTrue(values.length > 0);
    }

    @Test
    void testValueOf() {
        for (AccountStatus status : AccountStatus.values()) {
            assertEquals(status, AccountStatus.valueOf(status.name()));
        }
    }

    @Test
    void testEnumNotNull() {
        AccountStatus[] values = AccountStatus.values();
        for (AccountStatus status : values) {
            assertNotNull(status);
            assertNotNull(status.name());
        }
    }
}
