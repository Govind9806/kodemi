package com.example.kodemilabs.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.example.kodemilabs.model.OTP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OTPRepoTest {

    private DynamoDBMapper dynamoDBMapper;
    private OTPRepo otpRepo;

    private OTP otp;

    @BeforeEach
    void setUp() {
        dynamoDBMapper = Mockito.mock(DynamoDBMapper.class);
        otpRepo = new OTPRepo(dynamoDBMapper);

        otp = new OTP();
        otp.setUserId("user123"); // adjust if field name differs
    }

    // ✅ SAVE OTP
    @Test
    void save_shouldCallDynamoDBMapperSave() {
        otpRepo.save(otp);

        verify(dynamoDBMapper).save(otp);
    }

    // ✅ GET OTP SUCCESS
    @Test
    void getOtpData_shouldReturnOtp_whenExists() {
        when(dynamoDBMapper.load(OTP.class, "user123"))
                .thenReturn(otp);

        OTP result = otpRepo.getOtpData("user123");

        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        verify(dynamoDBMapper).load(OTP.class, "user123");
    }

    // ❌ GET OTP NOT FOUND
    @Test
    void getOtpData_shouldReturnNull_whenNotExists() {
        when(dynamoDBMapper.load(OTP.class, "user123"))
                .thenReturn(null);

        OTP result = otpRepo.getOtpData("user123");

        assertNull(result);
        verify(dynamoDBMapper).load(OTP.class, "user123");
    }

    // ✅ DELETE OTP
    @Test
    void delete_shouldCallDynamoDBMapperDelete() {
        otpRepo.delete(otp);

        verify(dynamoDBMapper).delete(otp);
    }
}