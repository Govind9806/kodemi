package com.example.course_service.controller;

import com.example.course_service.component.RequiresRole;
import com.example.course_service.dto.request.UploadInitRequest;
import com.example.course_service.dto.request.UploadPresignedUrlRequest;
import com.example.course_service.dto.request.UploadCompleteRequest;
import com.example.course_service.dto.request.UploadAbortRequest;
import com.example.course_service.dto.response.UploadInitResponse;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/uploads")
public class    UploadController {

    private final UploadService uploadService;
    private static final String MESSAGE = "message";

    @RequiresRole("TRAINER")
    @PostMapping("/multipart/init")
    public ResponseEntity<UploadInitResponse> initiateMultipartUpload(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid UploadInitRequest request) {
        log.info("[API] POST /uploads/multipart/init - fileType: {}", request.getFileType());
        return ResponseEntity.ok(uploadService.initiateMultipartUpload(token, request));
    }

    @RequiresRole("TRAINER")
    @PostMapping("/multipart/presigned-url")
    public ResponseEntity<Map<String, String>> getPresignedUrl(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid UploadPresignedUrlRequest request) {
        log.info("[API] POST /uploads/multipart/presigned-url - uploadId: {}, part: {}", request.getUploadId(), request.getPartNumber());
        String url = uploadService.generatePresignedUrl(token, request);
        return ResponseEntity.ok(Map.of("presignedUrl", url));
    }

    @RequiresRole("TRAINER")
    @PostMapping("/multipart/complete")
    public ResponseEntity<CompleteMultipartUploadResponse> completeMultipartUpload(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid UploadCompleteRequest request) {
        log.info("[API] POST /uploads/multipart/complete - uploadId: {}", request.getUploadId());
        return ResponseEntity.ok(uploadService.completeMultipartUpload(token, request));
    }

    @RequiresRole("TRAINER")
    @PostMapping("/multipart/abort")
    public ResponseEntity<Map<String, String>> abortMultipartUpload(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid UploadAbortRequest request) {
        log.info("[API] POST /uploads/multipart/abort - uploadId: {}", request.getUploadId());
        return ResponseEntity.ok(Map.of(MESSAGE, uploadService.abortMultipartUpload(token, request)));
    }
}
