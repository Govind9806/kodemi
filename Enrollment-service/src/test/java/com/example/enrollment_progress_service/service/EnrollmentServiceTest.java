package com.example.enrollment_progress_service.service;

import com.example.enrollment_progress_service.dto.request.EnrollmentRequest;
import com.example.enrollment_progress_service.dto.request.PaymentSuccessRequest;
import com.example.enrollment_progress_service.dto.request.UnenrollRequest;
import com.example.enrollment_progress_service.dto.response.*;
import com.example.enrollment_progress_service.enums.EnrollmentStatus;
import com.example.enrollment_progress_service.enums.EnrollmentTargetType;
import com.example.enrollment_progress_service.exception.BadRequestException;
import com.example.enrollment_progress_service.exception.ResourceNotFoundException;
import com.example.enrollment_progress_service.feign.CourseClient;
import com.example.enrollment_progress_service.feign.LiveClassClient;
import com.example.enrollment_progress_service.feign.NotificationClient;
import com.example.enrollment_progress_service.feign.PaymentClient;
import com.example.enrollment_progress_service.model.EnrollmentEntity;
import com.example.enrollment_progress_service.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseClient courseClient;
    @Mock
    private LiveClassClient liveClassClient;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private static final String USER_ID = "user-1";
    private static final String TARGET_ID = "target-1";
    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ================= enroll =================

    @Test
    void enroll_AlreadyEnrolled_ReturnsAlreadyEnrolledResponse() {
        EnrollmentEntity active = new EnrollmentEntity();
        active.setEnrollmentId("e1");
        active.setPaymentId("p1");

        when(enrollmentRepository.findActiveEnrollment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(active);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        EnrollmentResponse response = enrollmentService.enroll(USER_ID, request, TOKEN);
        assertEquals("Already enrolled", response.getMessage());
        assertEquals("e1", response.getEnrollmentId());
        assertEquals(EnrollmentStatus.ACTIVE, response.getStatus());
    }

    @Test
    void enroll_TargetFetchFails_ThrowsBadRequestException() {
        when(enrollmentRepository.findActiveEnrollment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(null);
        when(courseClient.getCourseEnrollmentInfo(TARGET_ID)).thenThrow(new RuntimeException("API error"));

        EnrollmentRequest request = new EnrollmentRequest();
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        assertThrows(BadRequestException.class, () -> enrollmentService.enroll(USER_ID, request, TOKEN));
    }

    @Test
    void enroll_TargetNotVerified_ThrowsBadRequestException() {
        when(enrollmentRepository.findActiveEnrollment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(null);
        
        EnrollmentItemInfoResponse itemInfo = new EnrollmentItemInfoResponse();
        itemInfo.setIsVerified(false);
        when(courseClient.getCourseEnrollmentInfo(TARGET_ID)).thenReturn(itemInfo);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        assertThrows(BadRequestException.class, () -> enrollmentService.enroll(USER_ID, request, TOKEN));
    }

    @Test
    void enroll_FreeItem_Success() {
        when(enrollmentRepository.findActiveEnrollment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(null);
        
        EnrollmentItemInfoResponse itemInfo = new EnrollmentItemInfoResponse();
        itemInfo.setIsVerified(true);
        itemInfo.setIsFree(true);
        when(courseClient.getCourseEnrollmentInfo(TARGET_ID)).thenReturn(itemInfo);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        EnrollmentResponse response = enrollmentService.enroll(USER_ID, request, TOKEN);
        assertEquals("Enrollment successful", response.getMessage());
        assertEquals(EnrollmentStatus.ACTIVE, response.getStatus());
        verify(enrollmentRepository).save(any(EnrollmentEntity.class));
    }

    @Test
    void enroll_PaidItem_VerifyPaymentFails_ThrowsBadRequestException() {
        when(enrollmentRepository.findActiveEnrollment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(null);
        
        EnrollmentItemInfoResponse itemInfo = new EnrollmentItemInfoResponse();
        itemInfo.setIsVerified(true);
        itemInfo.setIsFree(false);
        itemInfo.setPrice(BigDecimal.TEN);
        when(courseClient.getCourseEnrollmentInfo(TARGET_ID)).thenReturn(itemInfo);
        when(paymentClient.verifyPayment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenThrow(new RuntimeException("Payment service error"));

        EnrollmentRequest request = new EnrollmentRequest();
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        assertThrows(BadRequestException.class, () -> enrollmentService.enroll(USER_ID, request, TOKEN));
    }

    @Test
    void enroll_PaidItem_InsufficientAmount_ThrowsBadRequestException() {
        when(enrollmentRepository.findActiveEnrollment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(null);
        
        EnrollmentItemInfoResponse itemInfo = new EnrollmentItemInfoResponse();
        itemInfo.setIsVerified(true);
        itemInfo.setIsFree(false);
        itemInfo.setPrice(BigDecimal.TEN);
        when(courseClient.getCourseEnrollmentInfo(TARGET_ID)).thenReturn(itemInfo);

        PaymentVerificationResponse paymentResp = new PaymentVerificationResponse();
        paymentResp.setPaid(true);
        paymentResp.setAmount(BigDecimal.ONE);
        when(paymentClient.verifyPayment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(paymentResp);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        assertThrows(BadRequestException.class, () -> enrollmentService.enroll(USER_ID, request, TOKEN));
    }

    @Test
    void enroll_PaidItem_PaymentVerified_Success() {
        when(enrollmentRepository.findActiveEnrollment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(null);
        
        EnrollmentItemInfoResponse itemInfo = new EnrollmentItemInfoResponse();
        itemInfo.setIsVerified(true);
        itemInfo.setIsFree(false);
        itemInfo.setPrice(BigDecimal.TEN);
        when(courseClient.getCourseEnrollmentInfo(TARGET_ID)).thenReturn(itemInfo);

        PaymentVerificationResponse paymentResp = new PaymentVerificationResponse();
        paymentResp.setPaid(true);
        paymentResp.setAmount(BigDecimal.TEN);
        paymentResp.setPaymentId("pay-123");
        when(paymentClient.verifyPayment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(paymentResp);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        EnrollmentResponse response = enrollmentService.enroll(USER_ID, request, TOKEN);
        assertEquals("Enrollment successful", response.getMessage());
        assertEquals(EnrollmentStatus.ACTIVE, response.getStatus());
        assertEquals("pay-123", response.getPaymentId());
    }

    @Test
    void enroll_PaidItem_PaymentRequired_ReturnsPaymentRequiredResponse() {
        when(enrollmentRepository.findActiveEnrollment(USER_ID, TARGET_ID, EnrollmentTargetType.CONFERENCE.name()))
                .thenReturn(null);
        
        EnrollmentItemInfoResponse itemInfo = new EnrollmentItemInfoResponse();
        itemInfo.setIsVerified(true);
        itemInfo.setIsFree(false);
        itemInfo.setPrice(BigDecimal.TEN);
        when(liveClassClient.getConferenceEnrollmentInfo(TARGET_ID)).thenReturn(itemInfo);

        PaymentVerificationResponse paymentResp = new PaymentVerificationResponse();
        paymentResp.setPaid(false);
        when(paymentClient.verifyPayment(USER_ID, TARGET_ID, EnrollmentTargetType.CONFERENCE.name()))
                .thenReturn(paymentResp);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.CONFERENCE);

        EnrollmentResponse response = enrollmentService.enroll(USER_ID, request, TOKEN);
        assertEquals("Payment required", response.getMessage());
        assertEquals(EnrollmentStatus.PAYMENT_REQUIRED, response.getStatus());
        assertTrue(response.isPaymentRequired());
    }

    // ================= unenroll =================

    @Test
    void unenroll_ActiveEnrollmentNotFound_ThrowsResourceNotFoundException() {
        when(enrollmentRepository.findActiveEnrollment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(null);

        UnenrollRequest request = new UnenrollRequest();
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        assertThrows(ResourceNotFoundException.class, () -> enrollmentService.unenroll(USER_ID, request));
    }

    @Test
    void unenroll_Success() {
        EnrollmentEntity active = new EnrollmentEntity();
        active.setEnrollmentId("e1");
        active.setStatus(EnrollmentStatus.ACTIVE);

        when(enrollmentRepository.findActiveEnrollment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(active);

        UnenrollRequest request = new UnenrollRequest();
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        EnrollmentResponse response = enrollmentService.unenroll(USER_ID, request);
        assertEquals("Unenrolled successfully", response.getMessage());
        assertEquals(EnrollmentStatus.CANCELLED, response.getStatus());
        verify(enrollmentRepository).save(active);
    }

    // ================= getMyEnrollments =================

    @Test
    void getMyEnrollments_ReturnsList() {
        List<EnrollmentEntity> list = List.of(new EnrollmentEntity());
        when(enrollmentRepository.findByUserId(USER_ID)).thenReturn(list);

        assertEquals(list, enrollmentService.getMyEnrollments(USER_ID));
    }

    // ================= getStatus & checkAccess =================

    @Test
    void getStatus_InvalidType_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> enrollmentService.getStatus(USER_ID, TARGET_ID, "INVALID_TYPE"));
    }

    @Test
    void getStatus_Success() {
        EnrollmentEntity active = new EnrollmentEntity();
        when(enrollmentRepository.findActiveEnrollment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(active);

        EnrollmentStatusResponse response = enrollmentService.getStatus(USER_ID, TARGET_ID, "RECORDED_COURSE");
        assertTrue(response.isEnrolled());
        assertEquals(EnrollmentStatus.ACTIVE, response.getStatus());
    }

    @Test
    void checkAccess_InvalidType_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> enrollmentService.checkAccess(USER_ID, TARGET_ID, "INVALID_TYPE"));
    }

    @Test
    void checkAccess_Success() {
        when(enrollmentRepository.findActiveEnrollment(USER_ID, TARGET_ID, EnrollmentTargetType.RECORDED_COURSE.name()))
                .thenReturn(null);

        AccessCheckResponse response = enrollmentService.checkAccess(USER_ID, TARGET_ID, "RECORDED_COURSE");
        assertFalse(response.isHasAccess());
    }

    // ================= activateEnrollmentAfterPayment =================

    @Test
    void activateEnrollmentAfterPayment_InvalidStatus_ThrowsBadRequestException() {
        PaymentSuccessRequest request = new PaymentSuccessRequest();
        request.setStatus("FAILED");

        assertThrows(BadRequestException.class, () -> enrollmentService.activateEnrollmentAfterPayment(request));
    }

    @Test
    void activateEnrollmentAfterPayment_EnrollmentNotFound_CreatesNewActiveEnrollment() {
        PaymentSuccessRequest request = new PaymentSuccessRequest();
        request.setStatus("SUCCESS");
        request.setUserId(USER_ID);
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        when(enrollmentRepository.findByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, "RECORDED_COURSE"))
                .thenReturn(Collections.emptyList());

        PaymentVerificationResponse pResp = new PaymentVerificationResponse();
        pResp.setPaid(true);
        pResp.setAmount(new BigDecimal("10.00"));
        when(paymentClient.verifyPayment(eq(USER_ID), eq(TARGET_ID), anyString())).thenReturn(pResp);

        EnrollmentItemInfoResponse itemInfo = new EnrollmentItemInfoResponse();
        itemInfo.setPrice(new BigDecimal("10.00"));
        itemInfo.setIsVerified(true);
        when(courseClient.getCourseEnrollmentInfo(TARGET_ID)).thenReturn(itemInfo);

        EnrollmentResponse response = enrollmentService.activateEnrollmentAfterPayment(request);
        assertNotNull(response);
        assertEquals(EnrollmentStatus.ACTIVE, response.getStatus());
    }

    @Test
    void activateEnrollmentAfterPayment_AlreadyActive_ReturnsIgnoredResponse() {
        PaymentSuccessRequest request = new PaymentSuccessRequest();
        request.setStatus("SUCCESS");
        request.setUserId(USER_ID);
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        EnrollmentEntity active = new EnrollmentEntity();
        active.setStatus(EnrollmentStatus.ACTIVE);
        active.setEnrollmentId("e-active");

        when(enrollmentRepository.findByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, "RECORDED_COURSE"))
                .thenReturn(List.of(active));

        EnrollmentResponse response = enrollmentService.activateEnrollmentAfterPayment(request);
        assertEquals("Enrollment is already ACTIVE", response.getMessage());
        assertEquals(EnrollmentStatus.ACTIVE, response.getStatus());
    }

    @Test
    void activateEnrollmentAfterPayment_NoPaymentRequiredFound_ThrowsBadRequestException() {
        PaymentSuccessRequest request = new PaymentSuccessRequest();
        request.setStatus("SUCCESS");
        request.setUserId(USER_ID);
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        EnrollmentEntity cancelled = new EnrollmentEntity();
        cancelled.setStatus(EnrollmentStatus.CANCELLED);

        when(enrollmentRepository.findByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, "RECORDED_COURSE"))
                .thenReturn(List.of(cancelled));

        assertThrows(BadRequestException.class, () -> enrollmentService.activateEnrollmentAfterPayment(request));
    }

    @Test
    void activateEnrollmentAfterPayment_PaymentVerificationFails_ThrowsBadRequestException() {
        PaymentSuccessRequest request = new PaymentSuccessRequest();
        request.setStatus("SUCCESS");
        request.setUserId(USER_ID);
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        EnrollmentEntity pending = new EnrollmentEntity();
        pending.setStatus(EnrollmentStatus.PAYMENT_REQUIRED);

        when(enrollmentRepository.findByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, "RECORDED_COURSE"))
                .thenReturn(List.of(pending));
        when(paymentClient.verifyPayment(USER_ID, TARGET_ID, "RECORDED_COURSE")).thenReturn(null);

        assertThrows(BadRequestException.class, () -> enrollmentService.activateEnrollmentAfterPayment(request));
    }

    @Test
    void activateEnrollmentAfterPayment_PaymentVerificationUnpaid_ThrowsBadRequestException() {
        PaymentSuccessRequest request = new PaymentSuccessRequest();
        request.setStatus("SUCCESS");
        request.setUserId(USER_ID);
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        EnrollmentEntity pending = new EnrollmentEntity();
        pending.setStatus(EnrollmentStatus.PAYMENT_REQUIRED);

        when(enrollmentRepository.findByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, "RECORDED_COURSE"))
                .thenReturn(List.of(pending));

        PaymentVerificationResponse paymentResp = new PaymentVerificationResponse();
        paymentResp.setPaid(false);
        when(paymentClient.verifyPayment(USER_ID, TARGET_ID, "RECORDED_COURSE")).thenReturn(paymentResp);

        assertThrows(BadRequestException.class, () -> enrollmentService.activateEnrollmentAfterPayment(request));
    }

    @Test
    void activateEnrollmentAfterPayment_AmountTooLow_ThrowsBadRequestException() {
        PaymentSuccessRequest request = new PaymentSuccessRequest();
        request.setStatus("SUCCESS");
        request.setUserId(USER_ID);
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);

        EnrollmentEntity pending = new EnrollmentEntity();
        pending.setStatus(EnrollmentStatus.PAYMENT_REQUIRED);

        when(enrollmentRepository.findByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, "RECORDED_COURSE"))
                .thenReturn(List.of(pending));

        PaymentVerificationResponse paymentResp = new PaymentVerificationResponse();
        paymentResp.setPaid(true);
        paymentResp.setAmount(BigDecimal.ONE);
        when(paymentClient.verifyPayment(USER_ID, TARGET_ID, "RECORDED_COURSE")).thenReturn(paymentResp);

        EnrollmentItemInfoResponse itemInfo = new EnrollmentItemInfoResponse();
        itemInfo.setPrice(BigDecimal.TEN);
        when(courseClient.getCourseEnrollmentInfo(TARGET_ID)).thenReturn(itemInfo);

        assertThrows(BadRequestException.class, () -> enrollmentService.activateEnrollmentAfterPayment(request));
    }

    @Test
    void activateEnrollmentAfterPayment_Success() {
        PaymentSuccessRequest request = new PaymentSuccessRequest();
        request.setStatus("SUCCESS");
        request.setUserId(USER_ID);
        request.setTargetId(TARGET_ID);
        request.setTargetType(EnrollmentTargetType.RECORDED_COURSE);
        request.setPaymentId("pay-123");
        request.setOrderId("ord-456");

        EnrollmentEntity pending = new EnrollmentEntity();
        pending.setStatus(EnrollmentStatus.PAYMENT_REQUIRED);
        pending.setEnrollmentId("e-pending");

        when(enrollmentRepository.findByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, "RECORDED_COURSE"))
                .thenReturn(List.of(pending));

        PaymentVerificationResponse paymentResp = new PaymentVerificationResponse();
        paymentResp.setPaid(true);
        paymentResp.setAmount(BigDecimal.TEN);
        when(paymentClient.verifyPayment(USER_ID, TARGET_ID, "RECORDED_COURSE")).thenReturn(paymentResp);

        EnrollmentItemInfoResponse itemInfo = new EnrollmentItemInfoResponse();
        itemInfo.setPrice(BigDecimal.TEN);
        when(courseClient.getCourseEnrollmentInfo(TARGET_ID)).thenReturn(itemInfo);

        CourseResponseDTO courseDetail = new CourseResponseDTO();
        courseDetail.setCreatorId("trainer-1");
        courseDetail.setTitle("Recorded Course");
        when(courseClient.getCourseDetail(TARGET_ID)).thenReturn(courseDetail);

        EnrollmentResponse response = enrollmentService.activateEnrollmentAfterPayment(request);
        assertEquals("Enrollment activated successfully", response.getMessage());
        assertEquals(EnrollmentStatus.ACTIVE, response.getStatus());
        verify(enrollmentRepository).save(pending);
    }

    // ================= getEnrolledLearners =================

    @Test
    void getEnrolledLearners_Success() {
        EnrollmentEntity active = new EnrollmentEntity();
        active.setStatus(EnrollmentStatus.ACTIVE);
        active.setUserId("user-active");

        EnrollmentEntity cancelled = new EnrollmentEntity();
        cancelled.setStatus(EnrollmentStatus.CANCELLED);
        cancelled.setUserId("user-cancelled");

        when(enrollmentRepository.findByTargetId(TARGET_ID)).thenReturn(List.of(active, cancelled));

        List<String> result = enrollmentService.getEnrolledLearners(TARGET_ID);
        assertEquals(1, result.size());
        assertEquals("user-active", result.get(0));
    }

    // ================= getEnrollmentStats & getBatchEnrollmentStats =================

    @Test
    void getEnrollmentStats_InvalidType_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> 
                enrollmentService.getEnrollmentStats(TARGET_ID, "INVALID_TARGET_TYPE"));
    }

    @Test
    void getEnrollmentStats_Success() {
        EnrollmentEntity e1 = new EnrollmentEntity();
        e1.setTargetId(TARGET_ID);
        e1.setTargetType(EnrollmentTargetType.RECORDED_COURSE);
        e1.setStatus(EnrollmentStatus.ACTIVE);

        EnrollmentEntity e2 = new EnrollmentEntity();
        e2.setTargetId(TARGET_ID);
        e2.setTargetType(EnrollmentTargetType.RECORDED_COURSE);
        e2.setStatus(EnrollmentStatus.CANCELLED);

        EnrollmentEntity e3 = new EnrollmentEntity();
        e3.setTargetId(TARGET_ID);
        e3.setTargetType(EnrollmentTargetType.RECORDED_COURSE);
        e3.setStatus(EnrollmentStatus.PAYMENT_REQUIRED);

        EnrollmentEntity e4 = new EnrollmentEntity();
        e4.setTargetId(TARGET_ID);
        e4.setTargetType(EnrollmentTargetType.RECORDED_COURSE);
        e4.setStatus(EnrollmentStatus.EXPIRED);

        // Different target type, should be filtered out
        EnrollmentEntity e5 = new EnrollmentEntity();
        e5.setTargetId(TARGET_ID);
        e5.setTargetType(EnrollmentTargetType.LIVE_COURSE);
        e5.setStatus(EnrollmentStatus.ACTIVE);

        when(enrollmentRepository.findByTargetId(TARGET_ID)).thenReturn(List.of(e1, e2, e3, e4, e5));

        CourseEnrollmentStatsResponse stats = enrollmentService.getEnrollmentStats(TARGET_ID, "RECORDED_COURSE");
        
        assertEquals(TARGET_ID, stats.getTargetId());
        assertEquals("RECORDED_COURSE", stats.getTargetType());
        assertEquals(1, stats.getActiveCount());
        assertEquals(1, stats.getCancelledCount());
        assertEquals(1, stats.getPaymentRequiredCount());
        assertEquals(1, stats.getExpiredCount());
        assertEquals(4, stats.getTotalCount());
    }

    @Test
    void getBatchEnrollmentStats_Success() {
        String target1 = "t1";
        String target2 = "t2";

        EnrollmentEntity e1 = new EnrollmentEntity();
        e1.setTargetId(target1);
        e1.setTargetType(EnrollmentTargetType.RECORDED_COURSE);
        e1.setStatus(EnrollmentStatus.ACTIVE);

        EnrollmentEntity e2 = new EnrollmentEntity();
        e2.setTargetId(target2);
        e2.setTargetType(EnrollmentTargetType.RECORDED_COURSE);
        e2.setStatus(EnrollmentStatus.ACTIVE);

        when(enrollmentRepository.findByTargetId(target1)).thenReturn(List.of(e1));
        when(enrollmentRepository.findByTargetId(target2)).thenReturn(List.of(e2));

        com.example.enrollment_progress_service.dto.request.BatchEnrollmentStatsRequest request = 
                new com.example.enrollment_progress_service.dto.request.BatchEnrollmentStatsRequest();
        request.setTargetIds(List.of(target1, target2));
        request.setTargetType("RECORDED_COURSE");

        java.util.Map<String, CourseEnrollmentStatsResponse> resultMap = enrollmentService.getBatchEnrollmentStats(request);

        assertNotNull(resultMap);
        assertEquals(2, resultMap.size());
        
        CourseEnrollmentStatsResponse stats1 = resultMap.get(target1);
        assertNotNull(stats1);
        assertEquals(1, stats1.getActiveCount());
        assertEquals(1, stats1.getTotalCount());

        CourseEnrollmentStatsResponse stats2 = resultMap.get(target2);
        assertNotNull(stats2);
        assertEquals(1, stats2.getActiveCount());
        assertEquals(1, stats2.getTotalCount());
    }
}

