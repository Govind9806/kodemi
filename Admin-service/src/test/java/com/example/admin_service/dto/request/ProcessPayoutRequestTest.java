package com.example.admin_service.dto.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessPayoutRequestTest {

    // ===== GETTERS & SETTERS =====
    @Test
    void testGettersAndSetters() {
        ProcessPayoutRequest request = new ProcessPayoutRequest();

        request.setPayoutId("p1");
        request.setAction("APPROVE");
        request.setRemarks("All good");

        assertEquals("p1", request.getPayoutId());
        assertEquals("APPROVE", request.getAction());
        assertEquals("All good", request.getRemarks());
    }

    // ===== NULL VALUES =====
    @Test
    void testNullValues() {
        ProcessPayoutRequest request = new ProcessPayoutRequest();

        assertNull(request.getPayoutId());
        assertNull(request.getAction());
        assertNull(request.getRemarks());
    }

    // ===== EMPTY STRINGS =====
    @Test
    void testEmptyValues() {
        ProcessPayoutRequest request = new ProcessPayoutRequest();

        request.setPayoutId("");
        request.setAction("");
        request.setRemarks("");

        assertEquals("", request.getPayoutId());
        assertEquals("", request.getAction());
        assertEquals("", request.getRemarks());
    }

    // ===== UPDATE VALUES =====
    @Test
    void testUpdateValues() {
        ProcessPayoutRequest request = new ProcessPayoutRequest();

        request.setPayoutId("p1");
        request.setAction("APPROVE");
        request.setRemarks("ok");

        // update values
        request.setAction("REJECT");
        request.setRemarks("invalid");

        assertEquals("REJECT", request.getAction());
        assertEquals("invalid", request.getRemarks());
    }

    // ===== EDGE CASE: LONG STRINGS =====
    @Test
    void testLongStrings() {
        ProcessPayoutRequest request = new ProcessPayoutRequest();

        String longText = "x".repeat(1000);

        request.setRemarks(longText);

        assertEquals(longText, request.getRemarks());
    }
}