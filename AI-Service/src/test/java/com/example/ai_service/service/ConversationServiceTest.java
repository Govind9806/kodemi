package com.example.ai_service.service;

import com.example.ai_service.dto.request.ConversationRequest;
import com.example.ai_service.dto.response.ConversationResponse;
import com.example.ai_service.exceptions.ConversationNotFound;
import com.example.ai_service.exceptions.ConversationServiceException;
import com.example.ai_service.model.Conversations;
import com.example.ai_service.repository.ConversationRepository;
import com.example.ai_service.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConversationServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private ConversationService conversationService;

    private AutoCloseable closeable;

    private static final String TOKEN = "Bearer token";

    private static final LocalDateTime FIXED_TIME =
            LocalDateTime.of(2025, Month.JANUARY, 1, 10, 0, 0);

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void createConversationSuccess() {
        ConversationRequest request = buildRequest();

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user1");

        assertDoesNotThrow(() ->
                conversationService.createConversation(request, TOKEN));

        verify(conversationRepository, times(1))
                .save(any(Conversations.class));
    }

    @Test
    void createConversationNullRequest() {
        assertThrows(
                NullPointerException.class,
                () -> conversationService.createConversation(null, TOKEN)
        );
    }

    @Test
    void createConversationRepositoryException() {
        ConversationRequest request = buildRequest();

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user1");

        doThrow(new RuntimeException())
                .when(conversationRepository)
                .save(any(Conversations.class));

        assertThrows(
                ConversationServiceException.class,
                () -> conversationService.createConversation(request, TOKEN)
        );
    }

    @Test
    void getConversationByIdSuccess() {
        Conversations conversation = buildConversation();

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user1");
        when(conversationRepository.getConversationById("conv1"))
                .thenReturn(conversation);

        ConversationResponse response =
                conversationService.getConversationById("conv1", TOKEN);

        assertNotNull(response);
        assertEquals("conv1", response.getConversationId());
    }

    @Test
    void getConversationByIdNotFound() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user1");
        when(conversationRepository.getConversationById("conv1"))
                .thenReturn(null);

        assertThrows(
                ConversationServiceException.class,
                () -> conversationService.getConversationById("conv1", TOKEN)
        );
    }

    @Test
    void getConversationByIdUnauthorized() {
        Conversations conversation = buildConversation();
        conversation.setUserId("otherUser");

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user1");
        when(conversationRepository.getConversationById("conv1"))
                .thenReturn(conversation);

        assertThrows(
                ConversationServiceException.class,
                () -> conversationService.getConversationById("conv1", TOKEN)
        );
    }

    @Test
    void getAllConversationSuccess() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user1");

        when(conversationRepository.getUserConversations("user1"))
                .thenReturn(List.of(buildConversation()));

        List<ConversationResponse> responses =
                conversationService.getAllConversation(TOKEN);

        assertEquals(1, responses.size());
    }

    @Test
    void getAllConversationEmpty() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user1");

        when(conversationRepository.getUserConversations("user1"))
                .thenReturn(List.of());

        List<ConversationResponse> responses =
                conversationService.getAllConversation(TOKEN);

        assertTrue(responses.isEmpty());
    }

    @Test
    void deleteConversationSuccess() {
        Conversations conversation = buildConversation();

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user1");
        when(conversationRepository.getConversationById("conv1"))
                .thenReturn(conversation);

        assertDoesNotThrow(() ->
                conversationService.deleteConversation("conv1", TOKEN));

        verify(conversationRepository).delete(conversation);
    }

    @Test
    void deleteConversationNotFound() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user1");
        when(conversationRepository.getConversationById("conv1"))
                .thenReturn(null);

        assertThrows(
                ConversationNotFound.class,
                () -> conversationService.deleteConversation("conv1", TOKEN)
        );
    }

    @Test
    void deleteConversationUnauthorized() {
        Conversations conversation = buildConversation();
        conversation.setUserId("otherUser");

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user1");
        when(conversationRepository.getConversationById("conv1"))
                .thenReturn(conversation);

        assertThrows(
                RuntimeException.class,
                () -> conversationService.deleteConversation("conv1", TOKEN)
        );
    }

    @Test
    void testToEntity() {
        Conversations conversation = new Conversations();
        ConversationRequest request = buildRequest();

        conversationService.toEntity(conversation, request);

        assertEquals(
                request.getConversationId(),
                conversation.getConversationId()
        );

        assertEquals(
                request.getBotType(),
                conversation.getBotType()
        );
    }

    @Test
    void testToResponse() {
        Conversations entity = buildConversation();
        ConversationResponse response = new ConversationResponse();

        conversationService.toResponse(response, entity);

        assertEquals(
                entity.getConversationId(),
                response.getConversationId()
        );

        assertEquals(
                entity.getUserId(),
                response.getUserId()
        );
    }

    private ConversationRequest buildRequest() {
        ConversationRequest request = new ConversationRequest();

        request.setConversationId("conv1");
        request.setBotType("LEARNER");
        request.setSessionId("session1");
        request.setConversationName("Test");
        request.setCreatedAt(FIXED_TIME);
        request.setUpdatedAt(FIXED_TIME);
        request.setLastMessageAt(FIXED_TIME);

        return request;
    }

    private Conversations buildConversation() {
        Conversations conversation = new Conversations();

        conversation.setConversationId("conv1");
        conversation.setUserId("user1");
        conversation.setBotType("LEARNER");
        conversation.setSessionId("session1");
        conversation.setConversationName("Test");
        conversation.setCreatedAt(FIXED_TIME);
        conversation.setUpdatedAt(FIXED_TIME);
        conversation.setLastMessageAt(FIXED_TIME);

        return conversation;
    }
}