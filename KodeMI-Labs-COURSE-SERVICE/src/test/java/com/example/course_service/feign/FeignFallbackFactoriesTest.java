package com.example.course_service.feign;

import com.example.course_service.dto.request.BroadcastNotificationRequest;
import com.example.course_service.dto.request.CreateConferenceRequest;
import com.example.course_service.dto.request.LiveSessionRequest;
import com.example.course_service.dto.request.NotificationRequest;
import com.example.course_service.dto.response.LiveSessionResponse;
import com.example.course_service.dto.response.TrainerResponseDTO;
import com.example.course_service.dto.response.UserEnrollmentResponse;
import com.example.course_service.exception.DownstreamServiceException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FeignFallbackFactoriesTest {

    @Test
    void testEnrollmentClientFallbackFactory() {
        EnrollmentClientFallbackFactory factory = new EnrollmentClientFallbackFactory();
        RuntimeException cause = new RuntimeException("Service down");
        EnrollmentClient client = factory.create(cause);

        assertThrows(DownstreamServiceException.class, () -> client.checkAccess("u1", "t1", "COURSE"));

        List<String> learners = client.getEnrolledLearners("c1");
        assertNotNull(learners);
        assertTrue(learners.isEmpty());

        List<UserEnrollmentResponse> enrollments = client.getUserEnrollmentsInternal("u1");
        assertNotNull(enrollments);
        assertTrue(enrollments.isEmpty());
    }

    @Test
    void testLiveClientFallbackFactory() {
        LiveClientFallbackFactory factory = new LiveClientFallbackFactory();
        RuntimeException cause = new RuntimeException("Service down");
        LiveClient client = factory.create(cause);

        LiveSessionRequest sessionRequest = new LiveSessionRequest();
        assertThrows(DownstreamServiceException.class, () -> client.createSession(sessionRequest));

        CreateConferenceRequest conferenceRequest = new CreateConferenceRequest();
        assertThrows(DownstreamServiceException.class, () -> client.createConference(conferenceRequest, "token"));

        LiveSessionResponse session = client.getSessionById("s1");
        assertNotNull(session);
        assertNull(session.getSessionId());

        List<LiveSessionResponse> sessions = client.getSessionsByCourse("c1");
        assertNotNull(sessions);
        assertTrue(sessions.isEmpty());
    }

    @Test
    void testNotificationClientFallbackFactory() {
        NotificationClientFallbackFactory factory = new NotificationClientFallbackFactory();
        RuntimeException cause = new RuntimeException("Service down");
        NotificationClient client = factory.create(cause);

        NotificationRequest notificationRequest = NotificationRequest.builder().build();
        Map<String, String> result1 = client.sendInternalNotification("token", notificationRequest);
        assertNotNull(result1);
        assertTrue(result1.isEmpty());

        BroadcastNotificationRequest broadcastRequest = BroadcastNotificationRequest.builder().build();
        Map<String, Object> result2 = client.broadcastNotification("token", broadcastRequest);
        assertNotNull(result2);
        assertTrue(result2.isEmpty());
    }

    @Test
    void testTrainerClientFallbackFactory() {
        TrainerClientFallbackFactory factory = new TrainerClientFallbackFactory();
        RuntimeException cause = new RuntimeException("Service down");
        TrainerClient client = factory.create(cause);

        TrainerResponseDTO trainer1 = client.getTrainer("token");
        assertNotNull(trainer1);
        assertEquals("Trainer", trainer1.getFullName());
        assertEquals("default.png", trainer1.getProfilePictureURL());

        TrainerResponseDTO trainer2 = client.getTrainerById("u123");
        assertNotNull(trainer2);
        assertEquals("u123", trainer2.getUserId());
        assertEquals("Trainer", trainer2.getFullName());
        assertEquals("default.png", trainer2.getProfilePictureURL());
    }
}