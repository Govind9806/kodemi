package com.example.ai_service.dto;

import com.example.ai_service.dto.response.ConversationResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

class ConversationResponseTest {

    @Test
    void lombokGetterSetterCoverage() {

        LocalDateTime now = LocalDateTime.of(2024, Month.MARCH, 15, 14, 30, 45);

        ConversationResponse response =
                new ConversationResponse();

        response.setConversationId("conv-1");
        response.setUserId("user-1");
        response.setBotType("LEARNER");
        response.setSessionId("session-1");
        response.setConversationName("Test");
        response.setCreatedAt(now);
        response.setUpdatedAt(now);
        response.setLastMessageAt(now);

        assertEquals(
                "conv-1",
                response.getConversationId()
        );

        assertEquals(
                "user-1",
                response.getUserId()
        );

        assertEquals(
                "LEARNER",
                response.getBotType()
        );

        assertEquals(
                "session-1",
                response.getSessionId()
        );

        assertEquals(
                "Test",
                response.getConversationName()
        );

        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
        assertEquals(now, response.getLastMessageAt());
    }

    @Test
    void defaultValuesShouldBeNull() {

        ConversationResponse response =
                new ConversationResponse();

        assertNull(response.getConversationId());
        assertNull(response.getUserId());
        assertNull(response.getBotType());
        assertNull(response.getSessionId());
        assertNull(response.getConversationName());
    }
}