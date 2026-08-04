package com.example.ai_service.dto;

import com.example.ai_service.dto.request.MessageRequest;
import com.example.ai_service.enums.BotType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MessageRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validMessageRequest() {

        MessageRequest request =
                createValidRequest();

        assertTrue(
                validator.validate(request).isEmpty()
        );
    }

    @Test
    void allFieldsMissingShouldFailValidation() {

        MessageRequest request =
                new MessageRequest();

        Set<ConstraintViolation<MessageRequest>> violations =
                validator.validate(request);

        assertEquals(4, violations.size());
    }

    @Test
    void conversationIdShouldFailValidation() {
        MessageRequest request = createValidRequest();
        request.setConversationId("");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void userIdShouldFailValidation() {
        MessageRequest request = createValidRequest();
        request.setUserId("");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void contentShouldFailValidation() {
        MessageRequest request = createValidRequest();
        request.setContent("");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void botTypeShouldFailValidation() {
        MessageRequest request = createValidRequest();
        request.setBotType(null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void lombokGetterSetterCoverage() {

        MessageRequest request =
                new MessageRequest();

        request.setConversationId("conv-1");
        request.setUserId("user-1");
        request.setContent("hello");
        request.setBotType(BotType.LEARNER);

        assertEquals(
                "conv-1",
                request.getConversationId()
        );

        assertEquals(
                "user-1",
                request.getUserId()
        );

        assertEquals(
                "hello",
                request.getContent()
        );

        assertEquals(
                BotType.LEARNER,
                request.getBotType()
        );
    }

    private MessageRequest createValidRequest() {

        MessageRequest request =
                new MessageRequest();

        request.setConversationId("conv-1");
        request.setUserId("user-1");
        request.setContent("hello");
        request.setBotType(BotType.LEARNER);

        return request;
    }
}