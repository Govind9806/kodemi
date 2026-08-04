package com.example.ai_service.dto;

import com.example.ai_service.dto.request.ConversationRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConversationRequestTest {

    private Validator validator;

    private static final LocalDateTime FIXED_TIME =
            LocalDateTime.of(2024, Month.MARCH, 15, 14, 30, 45);

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validConversationRequest() {
        ConversationRequest request = createValidRequest();

        Set<ConstraintViolation<ConversationRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void allFieldsMissingShouldFailValidation() {
        ConversationRequest request = new ConversationRequest();

        Set<ConstraintViolation<ConversationRequest>> violations =
                validator.validate(request);

        assertEquals(7, violations.size());
    }

    @Test
    void conversationIdShouldFailValidation() {
        ConversationRequest request = createValidRequest();
        request.setConversationId("");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void botTypeShouldFailValidation() {
        ConversationRequest request = createValidRequest();
        request.setBotType("");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void sessionIdShouldFailValidation() {
        ConversationRequest request = createValidRequest();
        request.setSessionId("");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void conversationNameShouldFailValidation() {
        ConversationRequest request = createValidRequest();
        request.setConversationName("");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void createdAtShouldFailValidation() {
        ConversationRequest request = createValidRequest();
        request.setCreatedAt(null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void updatedAtShouldFailValidation() {
        ConversationRequest request = createValidRequest();
        request.setUpdatedAt(null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void lastMessageAtShouldFailValidation() {
        ConversationRequest request = createValidRequest();
        request.setLastMessageAt(null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void lombokGetterSetterCoverage() {
        ConversationRequest request = new ConversationRequest();

        request.setConversationId("conv");
        request.setBotType("LEARNER");
        request.setSessionId("session");
        request.setConversationName("name");
        request.setCreatedAt(FIXED_TIME);
        request.setUpdatedAt(FIXED_TIME);
        request.setLastMessageAt(FIXED_TIME);

        assertEquals("conv", request.getConversationId());
        assertEquals("LEARNER", request.getBotType());
        assertEquals("session", request.getSessionId());
        assertEquals("name", request.getConversationName());
        assertEquals(FIXED_TIME, request.getCreatedAt());
        assertEquals(FIXED_TIME, request.getUpdatedAt());
        assertEquals(FIXED_TIME, request.getLastMessageAt());
    }

    private ConversationRequest createValidRequest() {
        ConversationRequest request = new ConversationRequest();

        request.setConversationId("conv-1");
        request.setBotType("LEARNER");
        request.setSessionId("session-1");
        request.setConversationName("Test");
        request.setCreatedAt(FIXED_TIME);
        request.setUpdatedAt(FIXED_TIME);
        request.setLastMessageAt(FIXED_TIME);

        return request;
    }
}