package com.example.course_service.service.impl;

import com.example.course_service.dto.request.MultipartUploadPartETag;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.exception.FileUploadException;
import com.example.course_service.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final Duration PART_PRESIGN_DURATION = Duration.ofHours(4);

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final S3TransferManager transferManager;

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry-minutes:240}")
    private int expiryMinutes;

    @Value("${cloud.aws.cloudfront.domain:}")
    private String cloudFrontDomain;

    @Value("${cloud.aws.cloudfront.key-pair-id:}")
    private String cloudFrontKeyPairId;

    @Value("${cloud.aws.cloudfront.private-key-path:}")
    private String cloudFrontPrivateKeyPath;

    public FileServiceImpl(S3Client s3Client, S3Presigner presigner, S3TransferManager transferManager) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.transferManager = transferManager;
    }

    @Override
    public String generateUploadUrl(String key, String contentType) {
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .putObjectRequest(r -> r.bucket(bucketName).key(key).contentType(contentType))
                .build();

        return presigner.presignPutObject(presignRequest).url().toString();
    }

    @Override
    public String generatePresignedUrl(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiryMinutes))
                    .getObjectRequest(r -> r.bucket(bucketName).key(key))
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        } catch (SdkException e) {
            log.error("Failed to generate S3 presigned URL for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public String generateDownloadUrl(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        if (cloudFrontDomain == null || cloudFrontDomain.isEmpty()) {
            log.warn("CloudFront domain not configured, falling back to S3 presigned URL for key: {}", key);
            return generatePresignedUrl(key);
        }

        try {
            String resourceUrl = "https://" + cloudFrontDomain + "/" + key;
            Instant expiration = Instant.now().plus(Duration.ofMinutes(expiryMinutes));
            PrivateKey privateKey = getCloudFrontPrivateKey();

            CloudFrontUtilities cloudFrontUtilities = CloudFrontUtilities.create();
            CannedSignerRequest cannedSignerRequest = CannedSignerRequest.builder()
                    .resourceUrl(resourceUrl)
                    .privateKey(privateKey)
                    .keyPairId(cloudFrontKeyPairId)
                    .expirationDate(expiration)
                    .build();

            SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCannedPolicy(cannedSignerRequest);
            log.info("[CLOUDFRONT] Signed URL generated for key: {}", key);
            return signedUrl.url();
        } catch (IOException | GeneralSecurityException | SdkException e) {
            log.error("Failed to generate CloudFront signed URL for key {}: {}. Falling back to S3.", key, e.getMessage());
            return generatePresignedUrl(key);
        }
    }

    private PrivateKey getCloudFrontPrivateKey() throws IOException, GeneralSecurityException {
        Resource resource = new ClassPathResource(cloudFrontPrivateKeyPath);
        if (!resource.exists()) {
            throw new FileNotFoundException("CloudFront private key not found in classpath at: " + cloudFrontPrivateKeyPath);
        }

        try (InputStream is = resource.getInputStream()) {
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String privateKeyPem = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] encoded = Base64.getDecoder().decode(privateKeyPem);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            return keyFactory.generatePrivate(keySpec);
        }
    }

    @Override
    public void uploadFile(String key, InputStream inputStream, long fileSize, String contentType) {
        Path tempDir = null;
        Path tempFilePath = null;
        try {
            tempDir = createPrivateTempDirectory();
            tempFilePath = Files.createTempFile(tempDir, "s3-upload-", ".tmp");
            File tempFile = tempFilePath.toFile();
            copyToFile(inputStream, tempFile);

            Instant start = Instant.now();
            log.info("Starting optimized S3 upload for key: {}. Size: {} bytes", key, fileSize);

            UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                    .putObjectRequest(b -> b.bucket(bucketName).key(key).contentType(contentType))
                    .source(tempFile)
                    .build();

            FileUpload fileUpload = transferManager.uploadFile(uploadFileRequest);
            fileUpload.completionFuture().join();

            logUploadSpeed(key, fileSize, start);
        } catch (Exception e) {
            log.error("Upload failed for key {}: {}", key, e.getMessage());
            throw new FileUploadException("S3 Optimized Upload Failed: " + e.getMessage(), e, "unknown", key, 0);
        } finally {
            cleanupTempFiles(tempFilePath, tempDir);
        }
    }

    /**
     * Creates a private temp directory safely.
     * Uses POSIX atomic owner-only permissions on supported platforms.
     * Falls back to creating a unique directory under the user home directory on non-POSIX systems (e.g. Windows)
     * to avoid sharing a public OS temp location.
     */
    private Path createPrivateTempDirectory() throws IOException {
        try {
            FileAttribute<Set<PosixFilePermission>> ownerOnlyPermissions =
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
            return Files.createTempDirectory("s3-upload-dir-", ownerOnlyPermissions);
        } catch (UnsupportedOperationException e) {
            log.debug("POSIX permissions not supported on this OS, using user home directory fallback");
            Path userHomeDir = Paths.get(System.getProperty("user.home"), ".course-service-temp");
            if (!Files.exists(userHomeDir)) {
                Files.createDirectories(userHomeDir);
            }
            Path tempDir = userHomeDir.resolve("s3-upload-dir-" + UUID.randomUUID());
            Files.createDirectory(tempDir);

            File file = tempDir.toFile();
            boolean readable = file.setReadable(true, true);
            boolean writable = file.setWritable(true, true);
            boolean executable = file.setExecutable(true, true);
            if (!readable || !writable || !executable) {
                log.warn("Could not restrict permissions on temp directory: {}", tempDir);
            }
            return tempDir;
        }
    }

    private void copyToFile(InputStream inputStream, File tempFile) throws IOException {
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private void logUploadSpeed(String key, long fileSize, Instant start) {
        long duration = Duration.between(start, Instant.now()).toMillis();
        double speedMbps = fileSize > 0 && duration > 0 ? (fileSize * 8.0 / 1024 / 1024) / (duration / 1000.0) : 0;
        log.info("Upload completed in {}ms. Avg Speed: {} Mbps. Key: {}", duration,
                String.format(Locale.ROOT, "%.2f", speedMbps), key);
    }

    private void cleanupTempFiles(Path tempFilePath, Path tempDir) {
        if (tempFilePath != null) {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException e) {
                log.warn("Failed to delete temporary file: {}", tempFilePath);
            }
        }
        if (tempDir != null) {
            try {
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                log.warn("Failed to delete temporary directory: {}", tempDir);
            }
        }
    }

    @Override
    public void deleteFile(String key) {
        try {
            log.info("Deleting file from S3. Key: {}", key);
            s3Client.deleteObject(b -> b.bucket(bucketName).key(key));
        } catch (SdkException e) {
            log.error("Failed to delete file {}: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean fileExists(String key) {
        try {
            s3Client.headObject(b -> b.bucket(bucketName).key(key));
            return true;
        } catch (SdkException e) {
            return false;
        }
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public String initiateMultipartUpload(String key) {
        log.info("[MULTIPART INIT] Starting upload for key: {}", key);
        String uploadId = s3Client.createMultipartUpload(b -> b
                .bucket(bucketName)
                .key(key)
                .contentType(determineContentType(key))
        ).uploadId();
        log.info("[MULTIPART INIT SUCCESS] uploadId: {}, fileKey: {}", uploadId, key);
        return uploadId;
    }

    @Override
    public String generatePartUploadUrl(String fileKey, String uploadId, int partNumber) {
        log.info("[PRESIGNED URL] Generating for fileKey: {}, uploadId: {}, partNumber: {}", fileKey, uploadId, partNumber);
        UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                .signatureDuration(PART_PRESIGN_DURATION)
                .uploadPartRequest(r -> r.bucket(bucketName).key(fileKey).uploadId(uploadId).partNumber(partNumber))
                .build();
        String url = presigner.presignUploadPart(presignRequest).url().toString();
        log.info("[PRESIGNED URL SUCCESS] URL generated for part {}", partNumber);
        return url;
    }

    @Override
    public CompleteMultipartUploadResponse completeMultipartUpload(String key, String uploadId, List<MultipartUploadPartETag> parts) {
        log.info("[MULTIPART COMPLETE] Completing upload for key: {}, uploadId: {}", key, uploadId);
        List<MultipartUploadPartETag> sortedParts = new ArrayList<>(parts);
        sortedParts.sort(Comparator.comparingInt(MultipartUploadPartETag::getPartNumber));

        List<CompletedPart> completedParts = new ArrayList<>();
        for (MultipartUploadPartETag part : sortedParts) {
            completedParts.add(toCompletedPart(part));
        }

        s3Client.completeMultipartUpload(b -> b
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(m -> m.parts(completedParts))
        );

        log.info("[MULTIPART COMPLETE SUCCESS] Video key: {}, uploadId: {}", key, uploadId);
        return new CompleteMultipartUploadResponse(uploadId, key, "Multipart upload completed");
    }

    @Override
    public void abortMultipartUpload(String key, String uploadId) {
        s3Client.abortMultipartUpload(b -> b.bucket(bucketName).key(key).uploadId(uploadId));
    }

    private CompletedPart toCompletedPart(MultipartUploadPartETag part) {
        return CompletedPart.builder().partNumber(part.getPartNumber()).eTag(normalizeETag(part.getETag())).build();
    }

    private String normalizeETag(String eTag) {
        if (eTag == null) {
            return null;
        }
        String trimmed = eTag.trim();
        boolean isQuoted = (trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"));
        if (isQuoted) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String determineContentType(String key) {
        if (key == null) {
            return DEFAULT_CONTENT_TYPE;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        return DEFAULT_CONTENT_TYPE;
    }
}