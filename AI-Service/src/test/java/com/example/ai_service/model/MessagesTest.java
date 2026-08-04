package com.example.ai_service.model;

import com.example.ai_service.enums.BotType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MessagesTest {

    @Test
    void testMessagesGettersAndSetters() {

        Messages message = new Messages();

        LocalDateTime now = LocalDateTime.of(2024, java.time.Month.MARCH, 15, 14, 30, 45);

        message.setMessageId("msg123");
        message.setConversationId("conv123");
        message.setUserId("user123");
        message.setContent("Hello AI");
        message.setBotType(BotType.LEARNER);
        message.setCreatedAt(now.toString());

        assertEquals("msg123", message.getMessageId());
        assertEquals("conv123", message.getConversationId());
        assertEquals("user123", message.getUserId());
        assertEquals("Hello AI", message.getContent());
        assertEquals(BotType.LEARNER, message.getBotType());
        assertEquals(now.toString(), message.getCreatedAt());
    }

    @Test
    void testDefaultValues() {

        Messages message = new Messages();

        assertNull(message.getMessageId());
        assertNull(message.getConversationId());
        assertNull(message.getUserId());
        assertNull(message.getContent());
        assertNull(message.getBotType());
        assertNull(message.getCreatedAt());
    }

    @Test
    void testUpdateValues() {

        Messages message = new Messages();

        message.setContent("Old Content");
        assertEquals("Old Content", message.getContent());

        message.setContent("New Content");
        assertEquals("New Content", message.getContent());
    }

    @Test
    void testBotTypeValues() {

        Messages message = new Messages();

        message.setBotType(BotType.LEARNER);
        assertEquals(BotType.LEARNER, message.getBotType());

        message.setBotType(BotType.TRAINER);
        assertEquals(BotType.TRAINER, message.getBotType());
    }
}
