package com.example.course_service.service;

import com.example.course_service.dto.response.ModuleResponseDTO;
import com.example.course_service.exception.CourseNotFoundException;
import com.example.course_service.exception.ModuleNotFoundException;
import com.example.course_service.exception.NullException;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.model.ModuleEntity;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.ModuleRepository;
import com.example.course_service.service.impl.ModuleServiceImpl;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.service.notification.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModuleServiceImplTest {

    private ModuleServiceImpl moduleService;
    private ModuleRepository moduleRepository;
    private LessonRepository lessonRepository;
    private CourseRepository courseRepository;
    private FileService fileService;
    private EnrollmentClient enrollmentClient;
    private NotificationPublisher notificationPublisher;

    private ModuleEntity module;
    private CourseEntity course;

    @BeforeEach
    void setup() {
        moduleRepository = mock(ModuleRepository.class);
        lessonRepository = mock(LessonRepository.class);
        courseRepository = mock(CourseRepository.class);
        fileService = mock(FileService.class);
        enrollmentClient = mock(EnrollmentClient.class);
        notificationPublisher = mock(NotificationPublisher.class);

        moduleService = new ModuleServiceImpl(moduleRepository, lessonRepository, courseRepository, fileService, enrollmentClient, notificationPublisher);

        course = new CourseEntity();
        course.setCourseId("course-1");
        course.setTitle("Java Masterclass");
        course.setCreatorId("user-1");
        course.setCreatedAt(java.time.Instant.ofEpochMilli(1000000000000L));
        course.setUpdatedAt(java.time.Instant.ofEpochMilli(1000000000000L));

        module = new ModuleEntity();
        module.setModuleId("module-1");
        module.setCourseId("course-1");
        module.setUserId("user-1");
        module.setTitle("Introduction");
        module.setDescription("Module Description");
        module.setOrderIndex(1);
        module.setCreatedAt(java.time.Instant.ofEpochMilli(1000000000000L));
        module.setUpdatedAt(java.time.Instant.ofEpochMilli(1000000000000L));
    }

    // ================= CREATE =================

    @Test
    void createModule_Success() {
        when(courseRepository.findById("course-1")).thenReturn(course);

        ModuleEntity request = new ModuleEntity();
        request.setCourseId("course-1");
        request.setTitle("Introduction");
        request.setDescription("Module Description");
        request.setOrderIndex(1);

        java.util.Map<String, Object> response = moduleService.createModule(request, "user-1");

        assertTrue(response.get("message").toString().startsWith("Module Created Successfully"));

        ArgumentCaptor<ModuleEntity> captor = ArgumentCaptor.forClass(ModuleEntity.class);
        verify(moduleRepository, times(1)).save(captor.capture());

        ModuleEntity saved = captor.getValue();
        assertNotNull(saved.getModuleId());
        assertEquals("course-1", saved.getCourseId());
        assertEquals("user-1", saved.getUserId());
        assertEquals("Introduction", saved.getTitle());
    }

    @Test
    void createModule_NullBody_ThrowsException() {
        assertThrows(NullException.class, () -> moduleService.createModule(null, "user-1"));
        verify(moduleRepository, never()).save(any());
    }

    @Test
    void createModule_CourseNotFound_ThrowsException() {
        ModuleEntity request = new ModuleEntity();
        request.setCourseId("nonexistent");

        when(courseRepository.findById("nonexistent")).thenReturn(null);

        assertThrows(CourseNotFoundException.class,
                () -> moduleService.createModule(request, "user-1"));
    }

    @Test
    void createModule_NotOwner_ThrowsException() {
        when(courseRepository.findById("course-1")).thenReturn(course);

        ModuleEntity request = new ModuleEntity();
        request.setCourseId("course-1");

        assertThrows(RuntimeException.class,
                () -> moduleService.createModule(request, "hacker-user"));
    }

    // ================= GET BY ID =================

    @Test
    void getModuleById_Found_ReturnsDTO() {
        when(moduleRepository.findById("module-1")).thenReturn(module);
        when(lessonRepository.findByModuleId("module-1")).thenReturn(List.of());

        ModuleResponseDTO dto = moduleService.getModuleById("module-1");

        assertNotNull(dto);
        assertEquals("module-1", dto.getModuleId());
        assertEquals("Introduction", dto.getTitle());
    }

    @Test
    void getModuleById_NotFound_ThrowsException() {
        when(moduleRepository.findById("module-1")).thenReturn(null);

        assertThrows(ModuleNotFoundException.class,
                () -> moduleService.getModuleById("module-1"));
    }

    // ================= GET BY COURSE =================

    @Test
    void getModulesByCourse_ReturnsSortedList() {
        ModuleEntity m2 = new ModuleEntity();
        m2.setModuleId("module-2");
        m2.setCourseId("course-1");
        m2.setTitle("Advanced");
        m2.setOrderIndex(2);

        when(moduleRepository.findByCourseId("course-1")).thenReturn(List.of(module, m2));
        when(lessonRepository.findByModuleId("module-1")).thenReturn(List.of());
        when(lessonRepository.findByModuleId("module-2")).thenReturn(List.of());

        List<ModuleResponseDTO> list = moduleService.getModulesByCourse("course-1");

        assertEquals(2, list.size());
        assertEquals("Introduction", list.get(0).getTitle());
        assertEquals("Advanced", list.get(1).getTitle());
    }

    // ================= UPDATE =================

    @Test
    void updateModule_Success() {
        when(moduleRepository.findById("module-1")).thenReturn(module);

        ModuleEntity request = new ModuleEntity();
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");

        String response = moduleService.updateModule("user-1", "module-1", request);

        assertEquals("Module Updated Successfully.", response);
        verify(moduleRepository, times(1)).save(module);
        assertEquals("Updated Title", module.getTitle());
        assertEquals("Updated Description", module.getDescription());
    }

    @Test
    void updateModule_NullBody_ThrowsException() {
        assertThrows(NullException.class,
                () -> moduleService.updateModule("user-1", "module-1", null));
        verify(moduleRepository, never()).save(any());
    }

    @Test
    void updateModule_NotFound_ThrowsException() {
        when(moduleRepository.findById("module-1")).thenReturn(null);

        ModuleEntity request = new ModuleEntity();
        assertThrows(ModuleNotFoundException.class,
                () -> moduleService.updateModule("user-1", "module-1", request));
    }

    @Test
    void updateModule_NotOwner_ThrowsForbidden() {
        when(moduleRepository.findById("module-1")).thenReturn(module);

        ModuleEntity request = new ModuleEntity();
        request.setTitle("Hack");

        assertThrows(RuntimeException.class,
                () -> moduleService.updateModule("hacker-user", "module-1", request));
    }

    // ================= GET ALL =================

    @Test
    void getAllModules_ReturnsList() {
        when(moduleRepository.findAll()).thenReturn(List.of(module));
        when(lessonRepository.findByModuleId("module-1")).thenReturn(List.of());

        List<ModuleResponseDTO> result = moduleService.getAllModules();

        assertEquals(1, result.size());
        assertEquals("module-1", result.get(0).getModuleId());
    }

    @Test
    void getModulesByCourse_WithNullOrderIndex_HandledGracefully() {
        module.setOrderIndex(null);
        when(moduleRepository.findByCourseId("course-1")).thenReturn(List.of(module));
        when(lessonRepository.findByModuleId("module-1")).thenReturn(List.of());

        List<ModuleResponseDTO> result = moduleService.getModulesByCourse("course-1");

        assertEquals(1, result.size());
    }

    @Test
    void updateModule_OnlyTitle_UpdatesTitle() {
        when(moduleRepository.findById("module-1")).thenReturn(module);

        ModuleEntity request = new ModuleEntity();
        request.setTitle("New Title");
        // description and orderIndex are null - should not update them

        moduleService.updateModule("user-1", "module-1", request);

        assertEquals("New Title", module.getTitle());
        assertEquals("Module Description", module.getDescription()); // unchanged
    }

    @Test
    void updateModule_OnlyOrderIndex_UpdatesOrderIndex() {
        when(moduleRepository.findById("module-1")).thenReturn(module);

        ModuleEntity request = new ModuleEntity();
        request.setOrderIndex(5);

        moduleService.updateModule("user-1", "module-1", request);

        assertEquals(5, module.getOrderIndex());
    }
}