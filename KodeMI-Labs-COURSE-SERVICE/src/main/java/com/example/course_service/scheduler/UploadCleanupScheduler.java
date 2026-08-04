package com.example.course_service.scheduler;

import com.example.course_service.model.UploadEntity;
import com.example.course_service.repository.UploadRepository;
import com.example.course_service.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UploadCleanupScheduler {

    private final UploadRepository uploadRepository;
    private final FileService fileService;

    // Run every hour
    @Scheduled(fixedRate = 3600000)
    public void abortStaleIncompleteUploads() {
        log.info("[SCHEDULER] Running abortStaleIncompleteUploads");
        long sixHoursAgo = System.currentTimeMillis() - (6 * 60 * 60 * 1000);
        Date threshold = new Date(sixHoursAgo);

        List<UploadEntity> allUploads = uploadRepository.findAll();
        for (UploadEntity upload : allUploads) {
            if (("INITIATED".equals(upload.getUploadStatus()) || "UPLOADING".equals(upload.getUploadStatus())) 
                    && upload.getCreatedAt() != null 
                    && upload.getCreatedAt().before(threshold)) {
                try {
                    fileService.abortMultipartUpload(upload.getFileKey(), upload.getUploadId());
                    upload.setUploadStatus("ABORTED");
                    upload.setUpdatedAt(new Date());
                    uploadRepository.save(upload);
                    log.info("[SCHEDULER] Aborted stale upload: {}", upload.getUploadId());
                } catch (Exception e) {
                    log.error("[SCHEDULER] Failed to abort stale upload: {}", upload.getUploadId(), e);
                }
            }
        }
    }

    // Run daily
    @Scheduled(fixedRate = 86400000)
    public void deleteUnusedCompletedUploads() {
        log.info("[SCHEDULER] Running deleteUnusedCompletedUploads");
        long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        Date threshold = new Date(twentyFourHoursAgo);

        List<UploadEntity> allUploads = uploadRepository.findAll();
        for (UploadEntity upload : allUploads) {
            if ("COMPLETED".equals(upload.getUploadStatus()) 
                    && Boolean.FALSE.equals(upload.getIsUsed()) 
                    && upload.getUpdatedAt() != null 
                    && upload.getUpdatedAt().before(threshold)) {
                try {
                    fileService.deleteFile(upload.getFileKey());
                    upload.setUploadStatus("ABORTED");
                    upload.setUpdatedAt(new Date());
                    uploadRepository.save(upload);
                    log.info("[SCHEDULER] Deleted unused completed upload: {}", upload.getUploadId());
                } catch (Exception e) {
                    log.error("[SCHEDULER] Failed to delete unused upload: {}", upload.getUploadId(), e);
                }
            }
        }
    }
}
