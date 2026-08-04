package com.example.kodemilabs.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OTPTest {

    @Test
    void testGettersAndSetters() {
        OTP otp = new OTP();

        // Set values
        otp.setOtpCode("123456");
        otp.setUserId("user1");
        otp.setExpireAt(1678901234L);
        otp.setEnable(true);

        // Verify getters
        assertEquals("123456", otp.getOtpCode());
        assertEquals("user1", otp.getUserId());
        assertEquals(1678901234L, otp.getExpireAt());
        assertTrue(otp.isEnable());
    }

    @Test
    void testEqualsHashCodeAndToString() {
        OTP otp1 = new OTP();
        otp1.setOtpCode("123456");
        otp1.setUserId("user1");
        otp1.setExpireAt(1678901234L);
        otp1.setEnable(true);

        OTP otp2 = new OTP();
        otp2.setOtpCode("123456");
        otp2.setUserId("user1");
        otp2.setExpireAt(1678901234L);
        otp2.setEnable(true);

        // equals/hashCode
        assertEquals(otp1, otp2);
        assertEquals(otp1.hashCode(), otp2.hashCode());

        // toString
        assertNotNull(otp1.toString());
    }
}