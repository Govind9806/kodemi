package com.example.course_service.service;

import com.example.course_service.dto.response.ModuleResponseDTO;
import com.example.course_service.model.ModuleEntity;
import com.example.course_service.model.LessonEntity;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.ModuleRepository;
import com.example.course_service.service.impl.ModuleServiceImpl;
import com.example.course_service.service.FileService;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.service.notification.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModuleServiceAdditionalTest {

    private ModuleServiceImpl moduleService;
    private ModuleRepository moduleRepository;
    private LessonRepository lessonRepository;
    private CourseRepository courseRepository;
    private FileService fileService;
    private EnrollmentClient enrollmentClient;
    private NotificationPublisher notificationPublisher;

    @BeforeEach
    void setup() {
        moduleRepository = mock(ModuleRepository.class);
        lessonRepository = mock(LessonRepository.class);
        courseRepository = mock(CourseRepository.class);
        fileService = mock(FileService.class);
        enrollmentClient = mock(EnrollmentClient.class);
        notificationPublisher = mock(NotificationPublisher.class);
        moduleService = new ModuleServiceImpl(moduleRepository, lessonRepository, courseRepository, fileService, enrollmentClient, notificationPublisher);
    }

    @Test
    void getAllModules_ReturnsList() {
        ModuleEntity m1 = new ModuleEntity();
        m1.setModuleId("M1");
        m1.setTitle("Module 1");

        ModuleEntity m2 = new ModuleEntity();
        m2.setModuleId("M2");
        m2.setTitle("Module 2");

        when(moduleRepository.findAll()).thenReturn(List.of(m1, m2));
        when(lessonRepository.findByModuleId("M1")).thenReturn(List.of());
        when(lessonRepository.findByModuleId("M2")).thenReturn(List.of());

        List<ModuleResponseDTO> result = moduleService.getAllModules();

        assertEquals(2, result.size());
        assertEquals("Module 1", result.get(0).getTitle());
    }

    @Test
    void toDTO_SortingLessonsAndMappingVideoKey() {
        ModuleEntity m = new ModuleEntity();
        m.setModuleId("M1");
        m.setCourseId("C1");
        m.setTitle("Module 1");

        LessonEntity l1 = new LessonEntity();
        l1.setLessonId("L1");
        l1.setModuleId("M1");
        l1.setTitle("Lesson 1");
        l1.setOrderIndex(null); // defaults to 0
        l1.setVideoKey(null);

        LessonEntity l2 = new LessonEntity();
        l2.setLessonId("L2");
        l2.setModuleId("M1");
        l2.setTitle("Lesson 2");
        l2.setOrderIndex(3);
        l2.setVideoKey("video-2");

        LessonEntity l3 = new LessonEntity();
        l3.setLessonId("L3");
        l3.setModuleId("M1");
        l3.setTitle("Lesson 3");
        l3.setOrderIndex(1);
        l3.setVideoKey("video-3");

        when(moduleRepository.findById("M1")).thenReturn(m);
        when(lessonRepository.findByModuleId("M1")).thenReturn(List.of(l1, l2, l3));
        when(fileService.generateDownloadUrl("video-2")).thenReturn("url-2");
        when(fileService.generateDownloadUrl("video-3")).thenReturn("url-3");

        ModuleResponseDTO dto = moduleService.getModuleById("M1");

        assertNotNull(dto);
        assertEquals(3, dto.getLessons().size());
        
        // Sorting check: L1 (null->0) should be index 0, L3 (1) should be index 1, L2 (3) should be index 2
        assertEquals("L1", dto.getLessons().get(0).getLessonId());
        assertNull(dto.getLessons().get(0).getVideoUrl());

        assertEquals("L3", dto.getLessons().get(1).getLessonId());
        assertEquals("url-3", dto.getLessons().get(1).getVideoUrl());

        assertEquals("L2", dto.getLessons().get(2).getLessonId());
        assertEquals("url-2", dto.getLessons().get(2).getVideoUrl());
    }

    @Test
    void updateModule_UpdatesDescriptionOnly() {
        ModuleEntity existing = new ModuleEntity();
        existing.setModuleId("M1");
        existing.setUserId("user-1");
        existing.setTitle("Old Title");
        existing.setDescription("Old Description");

        ModuleEntity request = new ModuleEntity();
        request.setDescription("New Description");

        when(moduleRepository.findById("M1")).thenReturn(existing);

        String result = moduleService.updateModule("user-1", "M1", request);

        assertEquals("Module Updated Successfully.", result);
        assertEquals("New Description", existing.getDescription());
        assertEquals("Old Title", existing.getTitle());
        verify(moduleRepository, times(1)).save(existing);
    }
}
