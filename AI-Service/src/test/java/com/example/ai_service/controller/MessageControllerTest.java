package com.example.ai_service.controller;

import com.example.ai_service.dto.request.MessageRequest;
import com.example.ai_service.dto.response.MessageResponse;
import com.example.ai_service.exceptions.MessageServiceException;
import com.example.ai_service.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageController messageController;

    private static final String TOKEN = "Bearer token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createMessage_success() {

        MessageRequest request = new MessageRequest();

        doNothing().when(messageService)
                .createMessage(request, TOKEN);

        ResponseEntity<String> response =
                messageController.createMessage(request, TOKEN);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("Messages Created Successfully",
                response.getBody());

        verify(messageService).createMessage(request, TOKEN);
    }

    @Test
    void createMessage_illegalArgumentException() {

        MessageRequest request = new MessageRequest();

        doThrow(new IllegalArgumentException("Invalid input"))
                .when(messageService)
                .createMessage(request, TOKEN);

        ResponseEntity<String> response =
                messageController.createMessage(request, TOKEN);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid input", response.getBody());
    }

    @Test
    void createMessage_serviceException() {

        MessageRequest request = new MessageRequest();

        doThrow(new MessageServiceException("Service error"))
                .when(messageService)
                .createMessage(request, TOKEN);

        ResponseEntity<String> response =
                messageController.createMessage(request, TOKEN);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Failed to create messages",
                response.getBody());
    }

    @Test
    void createDBMessage_success() {

        MessageRequest request = new MessageRequest();

        doNothing().when(messageService)
                .createDBMessage(request);

        ResponseEntity<String> response =
                messageController.createDBMessage(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("Messages Created Successfully",
                response.getBody());
    }

    @Test
    void createDBMessage_illegalArgumentException() {

        MessageRequest request = new MessageRequest();

        doThrow(new IllegalArgumentException("Invalid input"))
                .when(messageService)
                .createDBMessage(request);

        ResponseEntity<String> response =
                messageController.createDBMessage(request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid input", response.getBody());
    }

    @Test
    void createDBMessage_serviceException() {

        MessageRequest request = new MessageRequest();

        doThrow(new MessageServiceException("Service error"))
                .when(messageService)
                .createDBMessage(request);

        ResponseEntity<String> response =
                messageController.createDBMessage(request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Failed to create messages",
                response.getBody());
    }

    @Test
    void getMessage_success() {

        MessageResponse messageResponse =
                new MessageResponse();

        when(messageService.getMessageById("msg-1", TOKEN))
                .thenReturn(messageResponse);

        ResponseEntity<MessageResponse> response =
                messageController.getMessage("msg-1", TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(messageResponse, response.getBody());
    }

    @Test
    void getMessage_nullResponse() {

        when(messageService.getMessageById("msg-1", TOKEN))
                .thenReturn(null);

        ResponseEntity<MessageResponse> response =
                messageController.getMessage("msg-1", TOKEN);

        assertNull(response.getBody());
    }

    @Test
    void getAllChats_success() {

        List<MessageResponse> messages =
                List.of(new MessageResponse());

        when(messageService.getAllMessage("conv-1", TOKEN))
                .thenReturn(messages);

        ResponseEntity<List<MessageResponse>> response =
                messageController.getAllChats("conv-1", TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(messages, response.getBody());
    }

    @Test
    void getAllChats_emptyList() {

        when(messageService.getAllMessage("conv-1", TOKEN))
                .thenReturn(List.of());

        ResponseEntity<List<MessageResponse>> response =
                messageController.getAllChats("conv-1", TOKEN);

        assertTrue(response.getBody().isEmpty());
    }
}