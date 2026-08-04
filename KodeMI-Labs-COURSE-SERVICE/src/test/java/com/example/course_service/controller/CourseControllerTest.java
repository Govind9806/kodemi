package com.example.course_service.controller;

import com.example.course_service.dto.response.CourseResponseDTO;
import com.example.course_service.service.CourseService;
import com.example.course_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CourseControllerTest {

    private MockMvc mockMvc;
    private CourseService courseService;
    private JwtUtil jwtUtil;
    private com.example.course_service.feign.EnrollmentClient enrollmentClient;

    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setup() {
        courseService = mock(CourseService.class);
        jwtUtil = mock(JwtUtil.class);
        enrollmentClient = mock(com.example.course_service.feign.EnrollmentClient.class);

        CourseController controller = new CourseController(courseService, jwtUtil, enrollmentClient);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private CourseResponseDTO buildDTO() {
        return CourseResponseDTO.builder()
                .courseId("C101")
                .title("Java Course")
                .creatorId("user-1")
                .categoryId("cat-1")
                .isVerified(true)
                .createdAt(new Date())
                .build();
    }

    // ================= REVIEW =================

    @Test
    void reviewCourse_Success() throws Exception {
        com.example.course_service.dto.request.CourseModerationRequest request = new com.example.course_service.dto.request.CourseModerationRequest();
        request.setCourseId("C101");
        request.setAction("VERIFY");
        
        when(courseService.moderateCourse(eq(TOKEN), eq("C101"), any(com.example.course_service.dto.request.CourseModerationRequest.class)))
                .thenReturn("Course Verified Successfully.");

        mockMvc.perform(put("/api/v1/course/review/C101")
                        .header("Authorization", TOKEN)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"VERIFY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Course Verified Successfully."))
                .andExpect(jsonPath("$.isVerified").value(true));
    }

    // ================= GET UNVERIFIED (STREAM) =================
    // Removed because the endpoint get-all-unverified is no longer exposed in the controller.

    // ================= GET ALL =================

    @Test
    void getAllCourses_Success() throws Exception {
        when(courseService.getAllCourses()).thenReturn(List.of(buildDTO()));

        mockMvc.perform(get("/api/v1/course/get-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value("C101"));
    }

    // ================= DETAIL =================

    @Test
    void getCourseDetail_Guest_Success() throws Exception {
        CourseResponseDTO dto = mock(CourseResponseDTO.class);
        when(dto.getCourseId()).thenReturn("C101");
        when(courseService.getCourseDetailWithModules("C101")).thenReturn(dto);
 
        mockMvc.perform(get("/api/v1/course/get-detail/C101"))
                .andExpect(status().isOk());
                
        verify(dto).maskVideoContent();
    }

    @Test
    void getCourseDetail_Organizer_Success() throws Exception {
        CourseResponseDTO course = buildDTO();
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1"); // Matches creatorId in buildDTO()
        when(courseService.getCourseDetailWithModules("C101")).thenReturn(course);

        mockMvc.perform(get("/api/v1/course/get-detail/C101")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value("C101"));

        verify(courseService, times(1)).getCourseDetailWithModules("C101");
    }

    @Test
    void getCourseDetail_Enrolled_Success() throws Exception {
        CourseResponseDTO course = buildDTO();
        course.setCreatorId("another-user");
        
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(courseService.getCourseDetailWithModules("C101")).thenReturn(course);
        when(enrollmentClient.checkAccess("user-1", "C101", "RECORDED_COURSE"))
                .thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(true).build());

        mockMvc.perform(get("/api/v1/course/get-detail/C101")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value("C101"));

        verify(courseService, times(1)).getCourseDetailWithModules("C101");
    }

    @Test
    void getCourseDetail_NotEnrolled_FallbackToPublic() throws Exception {
        CourseResponseDTO dto = mock(CourseResponseDTO.class);
        when(dto.getCourseId()).thenReturn("C101");
        
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(courseService.getCourseDetailWithModules("C101")).thenReturn(dto);
        when(enrollmentClient.checkAccess("user-1", "C101", "RECORDED_COURSE"))
                .thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(false).build());

        mockMvc.perform(get("/api/v1/course/get-detail/C101")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk());

        verify(dto).maskVideoContent();
    }

    // ================= BY CATEGORY =================

    @Test
    void getCoursesByCategory_Success() throws Exception {
        when(courseService.getCoursesByCategory("cat-1")).thenReturn(List.of(buildDTO()));

        mockMvc.perform(get("/api/v1/course/get-by-category/cat-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value("C101"));
    }

    // ================= REFRESH RATING =================

    @Test
    void refreshRating_Success() throws Exception {
        mockMvc.perform(post("/api/v1/course/internal/refresh-rating/C101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rating refreshed."));

        verify(courseService).refreshRatingCache("C101");
    }

    // ================= REFRESH STATS =================

    @Test
    void refreshStats_Success() throws Exception {
        mockMvc.perform(post("/api/v1/course/internal/refresh-stats/C101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Stats refreshed."));

        verify(courseService).refreshCourseStats("C101");
    }

    // ================= INSTRUCTOR COURSES =================

    @Test
    void getInstructorCourses_Success() throws Exception {
        CourseResponseDTO dto = buildDTO();
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(courseService.getCoursesByCreatorId("user-1", null)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/course/instructor/courses")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value("C101"));
    }

    // ================= PROTECTED DETAIL =================

    @Test
    void getProtectedCourseDetail_Enrolled_Success() throws Exception {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(courseService.getCourseDetailWithModules("C101")).thenReturn(buildDTO());
        when(enrollmentClient.checkAccess("user-1", "C101", "RECORDED_COURSE")).thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(true).build());

        mockMvc.perform(get("/api/v1/course/get-detail/protected/C101")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value("C101"));
    }

    @Test
    void getProtectedCourseDetail_NotEnrolled_MasksContent() throws Exception {
        CourseResponseDTO dto = mock(CourseResponseDTO.class);
        when(dto.getCourseId()).thenReturn("C101");

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(courseService.getCourseDetailWithModules("C101")).thenReturn(dto);
        when(enrollmentClient.checkAccess("user-1", "C101", "RECORDED_COURSE")).thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(false).build());

        mockMvc.perform(get("/api/v1/course/get-detail/protected/C101")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk());

        verify(dto).maskVideoContent();
    }

    @Test
    void getProtectedCourseDetail_EnrollmentCheckFails_MasksContent() throws Exception {
        CourseResponseDTO dto = mock(CourseResponseDTO.class);
        when(dto.getCourseId()).thenReturn("C101");

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(courseService.getCourseDetailWithModules("C101")).thenReturn(dto);
        when(enrollmentClient.checkAccess("user-1", "C101", "RECORDED_COURSE"))
                .thenThrow(new RuntimeException("Feign error"));

        mockMvc.perform(get("/api/v1/course/get-detail/protected/C101")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk());

        verify(dto).maskVideoContent();
    }

    @Test
    void getInstructorCourses_FiltersCorrectly() throws Exception {
        CourseResponseDTO dto1 = CourseResponseDTO.builder().courseId("C1").creatorId("user-1").build();

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(courseService.getCoursesByCreatorId("user-1", null)).thenReturn(List.of(dto1));

        mockMvc.perform(get("/api/v1/course/instructor/courses")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value("C1"))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
