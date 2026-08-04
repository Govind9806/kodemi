package com.example.ai_service.controller;

import com.example.ai_service.dto.request.ConversationRequest;
import com.example.ai_service.dto.response.ConversationResponse;
import com.example.ai_service.exceptions.ConversationServiceException;
import com.example.ai_service.service.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConversationControllerTest {

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private ConversationController conversationController;

    private static final String TOKEN = "Bearer token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createConversation_success() {

        ConversationRequest request = new ConversationRequest();

        doNothing().when(conversationService)
                .createConversation(request, TOKEN);

        ResponseEntity<String> response =
                conversationController.createConversation(request, TOKEN);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("Conversation Created Successfully", response.getBody());

        verify(conversationService).createConversation(request, TOKEN);
        verifyNoMoreInteractions(conversationService);
    }

    @Test
    void createConversation_illegalArgumentException() {

        ConversationRequest request = new ConversationRequest();

        doThrow(new IllegalArgumentException("Invalid input"))
                .when(conversationService)
                .createConversation(request, TOKEN);

        ResponseEntity<String> response =
                conversationController.createConversation(request, TOKEN);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid input", response.getBody());
    }

    @Test
    void createConversation_serviceException() {

        ConversationRequest request = new ConversationRequest();

        doThrow(new ConversationServiceException("Service error"))
                .when(conversationService)
                .createConversation(request, TOKEN);

        ResponseEntity<String> response =
                conversationController.createConversation(request, TOKEN);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Failed to create conversation", response.getBody());
    }

    @Test
    void getConversation_success() {

        ConversationResponse conversationResponse =
                new ConversationResponse();

        when(conversationService.getConversationById("conv-1", TOKEN))
                .thenReturn(conversationResponse);

        ResponseEntity<ConversationResponse> response =
                conversationController.getConversation("conv-1", TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(conversationResponse, response.getBody());

        verify(conversationService)
                .getConversationById("conv-1", TOKEN);
    }

    @Test
    void getConversation_nullResponse() {

        when(conversationService.getConversationById("conv-1", TOKEN))
                .thenReturn(null);

        ResponseEntity<ConversationResponse> response =
                conversationController.getConversation("conv-1", TOKEN);

        assertNull(response.getBody());
    }

    @Test
    void getAllConversation_success() {

        List<ConversationResponse> conversations =
                List.of(new ConversationResponse());

        when(conversationService.getAllConversation(TOKEN))
                .thenReturn(conversations);

        ResponseEntity<List<ConversationResponse>> response =
                conversationController.getAllConversation(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(conversations, response.getBody());
    }

    @Test
    void getAllConversation_emptyList() {

        when(conversationService.getAllConversation(TOKEN))
                .thenReturn(List.of());

        ResponseEntity<List<ConversationResponse>> response =
                conversationController.getAllConversation(TOKEN);

        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void deleteConversation_success() {

        doNothing().when(conversationService)
                .deleteConversation("conv-1", TOKEN);

        ResponseEntity<String> response =
                conversationController.deleteConversation("conv-1", TOKEN);

        assertEquals(202, response.getStatusCode().value());
        assertEquals("Conversation Deleted Successfully",
                response.getBody());
    }

    @Test
    void deleteConversation_illegalArgumentException() {

        doThrow(new IllegalArgumentException("Invalid conversation id"))
                .when(conversationService)
                .deleteConversation("conv-1", TOKEN);

        ResponseEntity<String> response =
                conversationController.deleteConversation("conv-1", TOKEN);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid conversation id", response.getBody());
    }

    @Test
    void deleteConversation_serviceException() {

        doThrow(new ConversationServiceException("Delete failed"))
                .when(conversationService)
                .deleteConversation("conv-1", TOKEN);

        ResponseEntity<String> response =
                conversationController.deleteConversation("conv-1", TOKEN);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Failed to delete conversation",
                response.getBody());
    }
}