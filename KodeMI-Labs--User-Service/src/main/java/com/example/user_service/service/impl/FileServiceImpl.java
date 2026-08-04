package com.example.user_service.service.impl;

import com.example.user_service.model.Learner;
import com.example.user_service.repository.LearnerRepository;
import com.example.user_service.exception.FileException;
import com.example.user_service.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private final S3Client           s3Client;
    private final LearnerRepository  learnerRepository;
    private final S3Presigner        presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry-minutes:60}")
    private int expiryMinutes;

    @Value("${cloud.aws.cloudfront.domain:}")
    private String cloudFrontDomain;

    @Value("${cloud.aws.cloudfront.key-pair-id:}")
    private String cloudFrontKeyPairId;

    @Value("${cloud.aws.cloudfront.private-key-path:}")
    private String cloudFrontPrivateKeyPath;

    // 1 GB limit
    private static final long MAX_FILE_SIZE = 1L * 1024 * 1024 * 1024;
    private static final long MULTIPART_THRESHOLD = 100L * 1024 * 1024; // 100 MB
    private static final int CHUNK_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final int PARALLEL_THREADS = 6;

    public FileServiceImpl(S3Client s3Client,
                           LearnerRepository learnerRepository,
                           S3Presigner presigner) {
        this.s3Client          = s3Client;
        this.learnerRepository = learnerRepository;
        this.presigner         = presigner;
    }

    // ── Public API ───────────────────────────────────────────────────────

    @Override
    public String uploadFile(String userId, MultipartFile file) {
        validateFile(file);

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String fileKey = generateFileKey(userId, getFileExtension(filename));

        if (file.getSize() > MULTIPART_THRESHOLD) {
            return uploadMultipart(userId, file, fileKey);
        } else {
            return uploadSinglePart(userId, file, fileKey);
        }
    }

    @Override
    public boolean fileExists(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) return false;

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            throw new FileException("Error checking file existence: " + e.getMessage());
        }
    }

    public void deleteFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) return;

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build());
            log.info("File deleted: {}", fileKey);
        } catch (S3Exception e) {
            log.error("Error deleting file: {}", fileKey, e);
            throw new FileException("Failed to delete file: " + e.getMessage());
        }
    }

    public byte[] getObjectAsBytes(String userId) {
        Learner learner = learnerRepository.findByUserId(userId);

        if (learner == null || learner.getProfilePictureKey() == null) {
            throw new FileException("No file found for user");
        }

        String objectKey = learner.getProfilePictureKey();

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            try (var response = s3Client.getObject(request)) {
                return response.readAllBytes();
            }

        } catch (IOException e) {
            throw new FileException("Error reading file from S3");
        } catch (S3Exception e) {
            throw new FileException("Error retrieving file from S3");
        }
    }

    // ── Upload Strategies ────────────────────────────────────────────────

    /**
     * Single-part upload for files under 100 MB.
     * Streams directly — no heap allocation.
     */
    private String uploadSinglePart(String userId, MultipartFile file, String fileKey) {
        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileKey)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );
            log.info("Single-part upload complete: {} ({} MB)",
                    fileKey, file.getSize() / 1024 / 1024);
            return fileKey;

        } catch (IOException e) {
            log.error("Single-part upload failed for userId: {}", userId, e);
            throw new FileException("Upload failed: " + e.getMessage());
        }
    }


    private String uploadMultipart(String userId, MultipartFile file, String fileKey) {
        String uploadId = null;

        try (InputStream inputStream = file.getInputStream();
             ExecutorServiceWrapper executor = new ExecutorServiceWrapper(Executors.newFixedThreadPool(PARALLEL_THREADS))) {

            // Step 1 — Initiate multipart upload
            uploadId = s3Client.createMultipartUpload(
                    CreateMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(fileKey)
                            .contentType(file.getContentType())
                            .build()
            ).uploadId();

            log.info("Multipart upload initiated: {} | uploadId: {}", fileKey, uploadId);

            // Step 2 — Read chunks and submit parallel upload tasks
            List<Future<CompletedPart>> futures  = new ArrayList<>();
            byte[] buffer   = new byte[CHUNK_SIZE];
            int    partNum  = 1;
            int    bytesRead;
            final  String finalUploadId = uploadId;

            while ((bytesRead = inputStream.readNBytes(buffer, 0, CHUNK_SIZE)) > 0) {
                final byte[] chunk   = Arrays.copyOf(buffer, bytesRead);
                final int    partNo  = partNum++;

                futures.add(executor.submit(() -> uploadPart(
                        fileKey, finalUploadId, partNo, chunk
                )));
            }

            // Step 3 — Collect completed parts
            List<CompletedPart> completedParts = collectCompletedParts(futures, fileKey, uploadId);

            // Step 4 — Sort by part number (S3 requires ordered parts)
            completedParts.sort(Comparator.comparingInt(CompletedPart::partNumber));

            // Step 5 — Complete the multipart upload
            s3Client.completeMultipartUpload(
                    CompleteMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(fileKey)
                            .uploadId(uploadId)
                            .multipartUpload(m -> m.parts(completedParts))
                            .build()
            );

            log.info("Multipart upload complete: {} | {} parts | {} MB",
                    fileKey, completedParts.size(), file.getSize() / 1024 / 1024);
            return fileKey;

        } catch (FileException e) {
            throw e;
        } catch (Exception e) {
            // Abort to prevent incomplete multipart from incurring S3 storage costs
            abortMultipartUpload(fileKey, uploadId);
            log.error("Multipart upload failed for userId: {}", userId, e);
            throw new FileException("Multipart upload failed: " + e.getMessage());
        }
    }

    /**
     * Collects completed parts from futures, handling InterruptedException properly.
     */
    private List<CompletedPart> collectCompletedParts(List<Future<CompletedPart>> futures,
                                                       String fileKey,
                                                       String uploadId) throws FileException {
        List<CompletedPart> completedParts = new ArrayList<>();
        for (Future<CompletedPart> future : futures) {
            try {
                completedParts.add(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                abortMultipartUpload(fileKey, uploadId);
                throw new FileException("Multipart upload interrupted: " + e.getMessage());
            } catch (ExecutionException e) {
                abortMultipartUpload(fileKey, uploadId);
                throw new FileException("Part upload failed: " + e.getCause().getMessage());
            }
        }
        return completedParts;
    }

    /**
     * Wrapper to make ExecutorService AutoCloseable for try-with-resources.
     */
    private static class ExecutorServiceWrapper implements AutoCloseable {
        private final ExecutorService executor;

        ExecutorServiceWrapper(ExecutorService executor) {
            this.executor = executor;
        }

        @Override
        public void close() {
            executor.shutdownNow();
        }

        public <T> Future<T> submit(Callable<T> task) {
            return executor.submit(task);
        }
    }

    /**
     * Uploads a single part — runs inside a thread pool.
     */
    private CompletedPart uploadPart(String fileKey,
                                     String uploadId,
                                     int    partNumber,
                                     byte[] chunk) {
        UploadPartResponse response = s3Client.uploadPart(
                UploadPartRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength((long) chunk.length)
                        .build(),
                RequestBody.fromBytes(chunk)
        );

        log.info("Uploaded part {} | size: {} MB | eTag: {}",
                partNumber, chunk.length / 1024 / 1024, response.eTag());

        return CompletedPart.builder()
                .partNumber(partNumber)
                .eTag(response.eTag())
                .build();
    }

    /**
     * Aborts an in-progress multipart upload.
     * Called automatically on failure to avoid S3 storage charges.
     */
    private void abortMultipartUpload(String fileKey, String uploadId) {
        if (uploadId == null) return;

        try {
            s3Client.abortMultipartUpload(
                    AbortMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(fileKey)
                            .uploadId(uploadId)
                            .build()
            );
            log.warn("Multipart upload aborted: {} | uploadId: {}", fileKey, uploadId);
        } catch (S3Exception ex) {
            log.error("Failed to abort multipart upload: {} | uploadId: {}", fileKey, uploadId, ex);
        }
    }

    // ── Validation & Utils ───────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileException("File size exceeds 1 GB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new FileException("Invalid file type — Content-Type is missing");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String generateFileKey(String userId, String extension) {
        return userId + "/" + UUID.randomUUID() + "." + extension;
    }
    public String generatePresignedUrl(String key) {

        if (key == null || key.isBlank()) {
            log.warn("Skipping URL generation because key is null or blank");
            return null;
        }

        if (key.startsWith("http://") || key.startsWith("https://")) {
            return key;
        }

        if (cloudFrontDomain != null && !cloudFrontDomain.isEmpty()
                && cloudFrontKeyPairId != null && !cloudFrontKeyPairId.isEmpty()
                && cloudFrontPrivateKeyPath != null && !cloudFrontPrivateKeyPath.isEmpty()) {
            try {
                String resourceUrl = "https://" + cloudFrontDomain + "/" + key;
                java.time.Instant expiration = java.time.Instant.now().plus(Duration.ofMinutes(expiryMinutes));
                java.security.PrivateKey privateKey = getCloudFrontPrivateKey();

                software.amazon.awssdk.services.cloudfront.CloudFrontUtilities cloudFrontUtilities =
                        software.amazon.awssdk.services.cloudfront.CloudFrontUtilities.create();
                software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest cannedSignerRequest =
                        software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest.builder()
                                .resourceUrl(resourceUrl)
                                .privateKey(privateKey)
                                .keyPairId(cloudFrontKeyPairId)
                                .expirationDate(expiration)
                                .build();

                software.amazon.awssdk.services.cloudfront.url.SignedUrl signedUrl =
                        cloudFrontUtilities.getSignedUrlWithCannedPolicy(cannedSignerRequest);
                log.info("[CLOUDFRONT] Signed URL generated for profile key: {}", key);
                return signedUrl.url();
            } catch (Exception e) {
                log.error("Failed to generate CloudFront signed URL for profile key {}: {}. Falling back to S3.", key, e.getMessage());
            }
        }

        try {
            GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiryMinutes))
                    .getObjectRequest(b -> b.bucket(bucketName).key(key))
                    .build();

            return presigner.presignGetObject(request)
                    .url()
                    .toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}", key, e);
            return key;
        }
    }

    private java.security.PrivateKey getCloudFrontPrivateKey() throws java.io.IOException, java.security.GeneralSecurityException {
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ClassPathResource(cloudFrontPrivateKeyPath);
        if (!resource.exists()) {
            throw new java.io.FileNotFoundException("CloudFront private key not found in classpath at: " + cloudFrontPrivateKeyPath);
        }

        try (java.io.InputStream is = resource.getInputStream()) {
            String pem = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                     .replace("-----END PRIVATE KEY-----", "")
                     .replaceAll("\\s+", "");
            byte[] encoded = java.util.Base64.getDecoder().decode(pem);
            java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(encoded);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpec);
        }
    }
}
