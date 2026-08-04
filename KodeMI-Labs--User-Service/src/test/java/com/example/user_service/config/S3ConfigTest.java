package com.example.user_service.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class S3ConfigTest {

    private S3Config s3Config;

    @BeforeEach
    void setUp() {
        s3Config = new S3Config();
        ReflectionTestUtils.setField(s3Config, "region", "eu-north-1");
    }

    @Test
    void s3ClientBeanShouldBeCreated() {
        S3Client s3Client = s3Config.s3Client();
        assertNotNull(s3Client, "S3Client bean should not be null");
        assertTrue(s3Client instanceof S3Client, "S3Client bean should be instance of S3Client");
    }

    @Test
    void s3PresignerBeanShouldBeCreated() {
        S3Presigner s3Presigner = s3Config.s3Presigner();
        assertNotNull(s3Presigner, "S3Presigner bean should not be null");
        assertTrue(s3Presigner instanceof S3Presigner, "S3Presigner bean should be instance of S3Presigner");
    }

    @Test
    void s3ClientIsOfCorrectType() {
        S3Client s3Client = s3Config.s3Client();
        assertTrue(s3Client instanceof S3Client, "S3Client bean should be of correct class");
    }

    @Test
    void s3PresignerIsOfCorrectType() {
        S3Presigner s3Presigner = s3Config.s3Presigner();
        assertTrue(s3Presigner instanceof S3Presigner, "S3Presigner bean should be of correct class");
    }
}