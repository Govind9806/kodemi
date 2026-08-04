package com.example.ai_service.exception;

import com.example.ai_service.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleConversationNotFound() {

        ResponseEntity<Map<String,Object>> response =
                handler.handleUserNotFound(
                        new ConversationNotFound("Conversation not found"));

        assertEquals(404, response.getStatusCode().value());

        Map<String,Object> body = response.getBody();

        assertNotNull(body);
        assertEquals("Conversation not found", body.get("message"));
        assertEquals(404, body.get("status"));
        assertEquals("Not Found", body.get("error"));
        assertTrue(body.containsKey("timestamp"));
    }

    @Test
    void handleConversationServiceException() {

        ResponseEntity<Map<String,Object>> response =
                handler.handleUserNotFound(
                        new ConversationServiceException("Service error"));

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void handleMessagesNotFound() {

        ResponseEntity<Map<String,Object>> response =
                handler.handleUserNotFound(
                        new MessagesNotFound("Messages not found"));

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void handleMessageServiceException() {

        ResponseEntity<Map<String,Object>> response =
                handler.handleUserNotFound(
                        new MessageServiceException("Message service error"));

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void handleInvalidRequest() {

        ResponseEntity<Map<String,Object>> response =
                handler.handleUserNotFound(
                        new InvalidRequest("Invalid request"));

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void handleUnauthorizedRequest() {

        ResponseEntity<Map<String,Object>> response =
                handler.handleUnAuthorizedRequestException(
                        new UnAuthorizedRequestException("Unauthorized"));

        assertEquals(401, response.getStatusCode().value());

        Map<String,Object> body = response.getBody();

        assertNotNull(body);
        assertEquals("Unauthorized", body.get("message"));
        assertEquals(401, body.get("status"));
        assertEquals("Unauthorized", body.get("error"));
    }
}