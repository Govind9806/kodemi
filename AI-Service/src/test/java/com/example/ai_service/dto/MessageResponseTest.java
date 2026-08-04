package com.example.ai_service.dto;

import com.example.ai_service.dto.response.MessageResponse;
import com.example.ai_service.enums.BotType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

class MessageResponseTest {

    @Test
    void lombokGetterSetterCoverage() {

        LocalDateTime now = LocalDateTime.of(2024, Month.MARCH, 15, 14, 30, 45);

        MessageResponse response =
                new MessageResponse();

        response.setMessageId("msg-1");
        response.setUserId("user-1");
        response.setConversationId("conv-1");
        response.setContent("Hello AI");
        response.setBotType(BotType.LEARNER);
        response.setCreatedAt(now);

        assertEquals("msg-1", response.getMessageId());
        assertEquals("user-1", response.getUserId());
        assertEquals("conv-1", response.getConversationId());
        assertEquals("Hello AI", response.getContent());
        assertEquals(BotType.LEARNER, response.getBotType());
        assertEquals(now, response.getCreatedAt());
    }

    @Test
    void defaultValuesShouldBeNull() {

        MessageResponse response =
                new MessageResponse();

        assertNull(response.getMessageId());
        assertNull(response.getUserId());
        assertNull(response.getConversationId());
        assertNull(response.getContent());
        assertNull(response.getBotType());
        assertNull(response.getCreatedAt());
    }
}