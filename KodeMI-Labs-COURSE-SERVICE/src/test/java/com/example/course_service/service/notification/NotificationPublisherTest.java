package com.example.course_service.service.notification;

import com.example.course_service.client.notification.NotificationClient;
import com.example.course_service.dto.notification.NotificationRequest;
import com.example.course_service.dto.notification.NotificationType;
import com.example.course_service.dto.request.BroadcastNotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class NotificationPublisherTest {

    private NotificationClient notificationClient;
    private NotificationPublisher notificationPublisher;

    @BeforeEach
    void setup() {
        notificationClient = mock(NotificationClient.class);
        notificationPublisher = new NotificationPublisher(notificationClient);
        ReflectionTestUtils.setField(notificationPublisher, "serviceKey", "test-key");
    }

    @Test
    void publish_Success() {
        NotificationRequest req = NotificationRequest.builder()
                .type(NotificationType.NEW_LESSON_ADDED)
                .userId("user1")
                .referenceId("ref1")
                .build();

        notificationPublisher.publish(req);

        verify(notificationClient, times(1)).sendInternalNotification("test-key", req);
    }

    @Test
    void publish_Exception_HandledGracefully() {
        doThrow(new RuntimeException("API error")).when(notificationClient).sendInternalNotification(any(), any());

        NotificationRequest req = NotificationRequest.builder()
                .type(NotificationType.NEW_LESSON_ADDED)
                .userId("user1")
                .referenceId("ref1")
                .build();

        assertDoesNotThrow(() -> notificationPublisher.publish(req));
    }

    @Test
    void publishBroadcast_Success() {
        BroadcastNotificationRequest req = BroadcastNotificationRequest.builder()
                .referenceId("ref1")
                .build();

        notificationPublisher.publishBroadcast(req);

        verify(notificationClient, times(1)).broadcastNotification("test-key", req);
    }

    @Test
    void publishBroadcast_Exception_HandledGracefully() {
        doThrow(new RuntimeException("API error")).when(notificationClient).broadcastNotification(any(), any());

        BroadcastNotificationRequest req = BroadcastNotificationRequest.builder().build();
        assertDoesNotThrow(() -> notificationPublisher.publishBroadcast(req));
    }

    @Test
    void publishToUsers_NullOrEmptyList() {
        notificationPublisher.publishToUsers(null, NotificationRequest.builder().build());
        notificationPublisher.publishToUsers(Collections.emptyList(), NotificationRequest.builder().build());

        verifyNoInteractions(notificationClient);
    }

    @Test
    void publishToUsers_MultipleUsers_WithExceptionOnOne() {
        when(notificationClient.sendInternalNotification(eq("test-key"), argThat(r -> "u1".equals(r.getUserId())))).thenReturn(Collections.emptyMap());
        doThrow(new RuntimeException("Failed")).when(notificationClient).sendInternalNotification(eq("test-key"), argThat(r -> "u2".equals(r.getUserId())));

        NotificationRequest req = NotificationRequest.builder()
                .type(NotificationType.NEW_LESSON_ADDED)
                .build();

        assertDoesNotThrow(() -> notificationPublisher.publishToUsers(List.of("u1", "u2"), req));

        verify(notificationClient, times(2)).sendInternalNotification(any(), any());
    }
}