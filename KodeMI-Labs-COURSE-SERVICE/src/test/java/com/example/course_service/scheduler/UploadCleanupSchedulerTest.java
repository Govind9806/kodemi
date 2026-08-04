package com.example.course_service.scheduler;

import com.example.course_service.model.UploadEntity;
import com.example.course_service.repository.UploadRepository;
import com.example.course_service.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class UploadCleanupSchedulerTest {

    private UploadRepository uploadRepository;
    private FileService fileService;
    private UploadCleanupScheduler scheduler;

    @BeforeEach
    void setup() {
        uploadRepository = mock(UploadRepository.class);
        fileService = mock(FileService.class);
        scheduler = new UploadCleanupScheduler(uploadRepository, fileService);
    }

    @Test
    void abortStaleIncompleteUploads_StaleUpload_Aborts() {
        UploadEntity staleUpload1 = new UploadEntity();
        staleUpload1.setUploadId("up1");
        staleUpload1.setFileKey("key1");
        staleUpload1.setUploadStatus("INITIATED");
        staleUpload1.setCreatedAt(new Date(System.currentTimeMillis() - (10 * 60 * 60 * 1000)));

        UploadEntity staleUpload2 = new UploadEntity();
        staleUpload2.setUploadId("up2");
        staleUpload2.setFileKey("key2");
        staleUpload2.setUploadStatus("UPLOADING");
        staleUpload2.setCreatedAt(new Date(System.currentTimeMillis() - (8 * 60 * 60 * 1000)));

        UploadEntity freshUpload = new UploadEntity();
        freshUpload.setUploadId("up3");
        freshUpload.setUploadStatus("INITIATED");
        freshUpload.setCreatedAt(new Date());

        UploadEntity completedUpload = new UploadEntity();
        completedUpload.setUploadId("up4");
        completedUpload.setUploadStatus("COMPLETED");

        when(uploadRepository.findAll()).thenReturn(List.of(staleUpload1, staleUpload2, freshUpload, completedUpload));

        scheduler.abortStaleIncompleteUploads();

        verify(fileService, times(1)).abortMultipartUpload("key1", "up1");
        verify(fileService, times(1)).abortMultipartUpload("key2", "up2");
        verify(uploadRepository, times(2)).save(any());
    }

    @Test
    void abortStaleIncompleteUploads_ExceptionHandled() {
        UploadEntity staleUpload = new UploadEntity();
        staleUpload.setUploadId("up1");
        staleUpload.setFileKey("key1");
        staleUpload.setUploadStatus("INITIATED");
        staleUpload.setCreatedAt(new Date(System.currentTimeMillis() - (10 * 60 * 60 * 1000)));

        when(uploadRepository.findAll()).thenReturn(List.of(staleUpload));
        doThrow(new RuntimeException("S3 error")).when(fileService).abortMultipartUpload(any(), any());

        assertDoesNotThrow(() -> scheduler.abortStaleIncompleteUploads());
    }

    @Test
    void deleteUnusedCompletedUploads_StaleUnusedCompleted_Deletes() {
        UploadEntity staleUnused = new UploadEntity();
        staleUnused.setUploadId("up1");
        staleUnused.setFileKey("key1");
        staleUnused.setUploadStatus("COMPLETED");
        staleUnused.setIsUsed(false);
        staleUnused.setUpdatedAt(new Date(System.currentTimeMillis() - (30 * 60 * 60 * 1000)));

        UploadEntity usedUpload = new UploadEntity();
        usedUpload.setUploadId("up2");
        usedUpload.setUploadStatus("COMPLETED");
        usedUpload.setIsUsed(true);
        usedUpload.setUpdatedAt(new Date(System.currentTimeMillis() - (30 * 60 * 60 * 1000)));

        when(uploadRepository.findAll()).thenReturn(List.of(staleUnused, usedUpload));

        scheduler.deleteUnusedCompletedUploads();

        verify(fileService, times(1)).deleteFile("key1");
        verify(uploadRepository, times(1)).save(staleUnused);
    }

    @Test
    void deleteUnusedCompletedUploads_ExceptionHandled() {
        UploadEntity staleUnused = new UploadEntity();
        staleUnused.setUploadId("up1");
        staleUnused.setFileKey("key1");
        staleUnused.setUploadStatus("COMPLETED");
        staleUnused.setIsUsed(false);
        staleUnused.setUpdatedAt(new Date(System.currentTimeMillis() - (30 * 60 * 60 * 1000)));

        when(uploadRepository.findAll()).thenReturn(List.of(staleUnused));
        doThrow(new RuntimeException("S3 error")).when(fileService).deleteFile(any());

        assertDoesNotThrow(() -> scheduler.deleteUnusedCompletedUploads());
    }
}
