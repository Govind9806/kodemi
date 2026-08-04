package com.example.ai_service.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

class ConversationsTest {

    @Test
    void testConversationsGettersAndSetters() {

        Conversations conversation = new Conversations();

        LocalDateTime now = LocalDateTime.of(2024, Month.MARCH, 15, 14, 30, 45);

        conversation.setConversationId("conv123");
        conversation.setUserId("user123");
        conversation.setBotType("LEARNER");
        conversation.setSessionId("session123");
        conversation.setConversationName("Test Conversation");
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversation.setLastMessageAt(now);

        assertEquals("conv123", conversation.getConversationId());
        assertEquals("user123", conversation.getUserId());
        assertEquals("LEARNER", conversation.getBotType());
        assertEquals("session123", conversation.getSessionId());
        assertEquals("Test Conversation", conversation.getConversationName());
        assertEquals(now, conversation.getCreatedAt());
        assertEquals(now, conversation.getUpdatedAt());
        assertEquals(now, conversation.getLastMessageAt());
    }

    @Test
    void testDefaultValues() {

        Conversations conversation = new Conversations();

        assertNull(conversation.getConversationId());
        assertNull(conversation.getUserId());
        assertNull(conversation.getBotType());
        assertNull(conversation.getSessionId());
        assertNull(conversation.getConversationName());
        assertNull(conversation.getCreatedAt());
        assertNull(conversation.getUpdatedAt());
        assertNull(conversation.getLastMessageAt());
    }

    @Test
    void testUpdateValues() {

        Conversations conversation = new Conversations();

        conversation.setConversationName("Old Name");
        assertEquals("Old Name", conversation.getConversationName());

        conversation.setConversationName("New Name");
        assertEquals("New Name", conversation.getConversationName());
    }
}
