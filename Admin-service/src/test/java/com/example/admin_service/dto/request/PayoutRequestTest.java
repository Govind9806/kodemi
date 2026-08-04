package com.example.admin_service.dto.request;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PayoutRequestTest {

    // ===== BUILDER TEST =====
    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();

        PayoutRequest request = PayoutRequest.builder()
                .payoutId("p1")
                .trainerId("t1")
                .amount(BigDecimal.valueOf(1000))
                .bankAccount("1234567890")
                .ifscCode("IFSC001")
                .accountHolderName("John Doe")
                .status("PENDING")
                .requestedAt(now)
                .processedAt(now)
                .processedBy("admin")
                .remarks("ok")
                .build();

        assertEquals("p1", request.getPayoutId());
        assertEquals("t1", request.getTrainerId());
        assertEquals(BigDecimal.valueOf(1000), request.getAmount());
        assertEquals("1234567890", request.getBankAccount());
        assertEquals("IFSC001", request.getIfscCode());
        assertEquals("John Doe", request.getAccountHolderName());
        assertEquals("PENDING", request.getStatus());
        assertEquals(now, request.getRequestedAt());
        assertEquals(now, request.getProcessedAt());
        assertEquals("admin", request.getProcessedBy());
        assertEquals("ok", request.getRemarks());
    }

    // ===== GETTERS & SETTERS =====
    @Test
    void testGettersAndSetters() {
        PayoutRequest request = new PayoutRequest();

        LocalDateTime now = LocalDateTime.now();

        request.setPayoutId("p2");
        request.setTrainerId("t2");
        request.setAmount(BigDecimal.valueOf(500));
        request.setBankAccount("9876543210");
        request.setIfscCode("IFSC002");
        request.setAccountHolderName("Jane Doe");
        request.setStatus("APPROVED");
        request.setRequestedAt(now);
        request.setProcessedAt(now);
        request.setProcessedBy("admin2");
        request.setRemarks("done");

        assertEquals("p2", request.getPayoutId());
        assertEquals("t2", request.getTrainerId());
        assertEquals(BigDecimal.valueOf(500), request.getAmount());
        assertEquals("9876543210", request.getBankAccount());
        assertEquals("IFSC002", request.getIfscCode());
        assertEquals("Jane Doe", request.getAccountHolderName());
        assertEquals("APPROVED", request.getStatus());
        assertEquals(now, request.getRequestedAt());
        assertEquals(now, request.getProcessedAt());
        assertEquals("admin2", request.getProcessedBy());
        assertEquals("done", request.getRemarks());
    }

    // ===== ALL ARGS CONSTRUCTOR =====
    @Test
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();

        PayoutRequest request = new PayoutRequest(
                "p3",
                "t3",
                BigDecimal.valueOf(750),
                "111222333",
                "IFSC003",
                "Alex",
                "REJECTED",
                now,
                now,
                "admin3",
                "invalid"
        );

        assertEquals("p3", request.getPayoutId());
        assertEquals("t3", request.getTrainerId());
        assertEquals(BigDecimal.valueOf(750), request.getAmount());
        assertEquals("111222333", request.getBankAccount());
        assertEquals("IFSC003", request.getIfscCode());
        assertEquals("Alex", request.getAccountHolderName());
        assertEquals("REJECTED", request.getStatus());
        assertEquals(now, request.getRequestedAt());
        assertEquals(now, request.getProcessedAt());
        assertEquals("admin3", request.getProcessedBy());
        assertEquals("invalid", request.getRemarks());
    }

    // ===== NO ARGS CONSTRUCTOR =====
    @Test
    void testNoArgsConstructor() {
        PayoutRequest request = new PayoutRequest();

        assertNull(request.getPayoutId());
        assertNull(request.getTrainerId());
        assertNull(request.getAmount());
        assertNull(request.getBankAccount());
        assertNull(request.getIfscCode());
        assertNull(request.getAccountHolderName());
        assertNull(request.getStatus());
        assertNull(request.getRequestedAt());
        assertNull(request.getProcessedAt());
        assertNull(request.getProcessedBy());
        assertNull(request.getRemarks());
    }

    // ===== NULL VALUES =====
    @Test
    void testNullValues() {
        PayoutRequest request = PayoutRequest.builder().build();

        assertNull(request.getPayoutId());
        assertNull(request.getTrainerId());
        assertNull(request.getAmount());
        assertNull(request.getStatus());
    }

    // ===== EDGE CASE: LARGE AMOUNT =====
    @Test
    void testLargeAmount() {
        PayoutRequest request = new PayoutRequest();
        request.setAmount(new BigDecimal("999999999999999999"));

        assertEquals(new BigDecimal("999999999999999999"), request.getAmount());
    }

    // ===== EDGE CASE: EMPTY STRINGS =====
    @Test
    void testEmptyStrings() {
        PayoutRequest request = new PayoutRequest();

        request.setPayoutId("");
        request.setTrainerId("");
        request.setStatus("");

        assertEquals("", request.getPayoutId());
        assertEquals("", request.getTrainerId());
        assertEquals("", request.getStatus());
    }
}