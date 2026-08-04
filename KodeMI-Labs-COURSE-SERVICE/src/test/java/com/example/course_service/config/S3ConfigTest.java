package com.example.course_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import static org.junit.jupiter.api.Assertions.*;

class S3ConfigTest {

    @Test
    void s3Client_BeanCreation_Success() {
        S3Config config = new S3Config();
        ReflectionTestUtils.setField(config, "region", "eu-north-1");

        S3Client client = config.s3Client();

        assertNotNull(client);
    }

    @Test
    void s3Presigner_BeanCreation_Success() {
        S3Config config = new S3Config();
        ReflectionTestUtils.setField(config, "region", "eu-north-1");

        S3Presigner presigner = config.s3Presigner();

        assertNotNull(presigner);
    }

    @Test
    void s3TransferManager_BeanCreation_Success() {
        S3Config config = new S3Config();
        ReflectionTestUtils.setField(config, "region", "eu-north-1");

        var asyncClient = config.s3AsyncClient();
        assertNotNull(asyncClient);

        S3TransferManager manager = config.s3TransferManager(asyncClient);
        assertNotNull(manager);
    }
}